/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.carselection;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputLayout;
import com.jakewharton.rxbinding3.appcompat.RxToolbar;
import com.jakewharton.rxbinding3.widget.RxTextView;

import org.envirocar.app.R;
import org.envirocar.app.handler.preferences.CarPreferenceHandler;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.views.utils.ECAnimationUtils;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.CarImpl;
import org.envirocar.core.entity.Vehicles;
import org.envirocar.core.logging.Logger;
import org.envirocar.storage.EnviroCarVehicleDB;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTextChanged;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class CarSelectionAddCarFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(CarSelectionAddCarFragment.class);

    private static final int ERROR_DEBOUNCE_TIME = 750;

    @BindView(R.id.envirocar_toolbar)
    protected Toolbar toolbar;
    @BindView(R.id.activity_car_selection_newcar_toolbar_exp)
    protected View toolbarExp;
    @BindView(R.id.activity_car_selection_newcar_content_view)
    protected View contentView;
    @BindView(R.id.activity_car_selection_newcar_download_layout)
    protected View downloadView;

    @BindView(R.id.activity_car_selection_newcar_layout_manufacturer)
    protected TextInputLayout manufacturerLayout;
    @BindView(R.id.activity_car_selection_newcar_input_manufacturer)
    protected AutoCompleteTextView manufacturerText;
    @BindView(R.id.activity_car_selection_newcar_input_model)
    protected AutoCompleteTextView modelText;
    @BindView(R.id.activity_car_selection_newcar_input_constructionyear)
    protected AutoCompleteTextView yearText;
    @BindView(R.id.activity_car_selection_newcar_input_fueltype)
    protected AutoCompleteTextView fueltypeText;
    @BindView(R.id.activity_car_selection_newcar_layout_engine)
    protected TextInputLayout engineLayout;
    @BindView(R.id.activity_car_selection_newcar_input_engine)
    protected AutoCompleteTextView engineText;

    @Inject
    protected DAOProvider daoProvider;
    @Inject
    protected CarPreferenceHandler carManager;
    @Inject
    protected EnviroCarVehicleDB enviroCarVehicleDB;

    private CompositeDisposable disposables = new CompositeDisposable();
    private Scheduler.Worker mainThreadWorker = AndroidSchedulers.mainThread().createWorker();

    private Set<Car> mCars = new HashSet<>();
    private Set<String> mManufacturerNames = new HashSet<>();
    private Map<String, Set<String>> mCarToModelMap = new ConcurrentHashMap<>();
    private Map<String, Set<String>> mModelToYear = new ConcurrentHashMap<>();
    private Map<Pair<String, String>, Set<String>> mModelToCCM = new ConcurrentHashMap<>();
    private Map<Pair<String, String>, Set<String>> mModelYearToFuel = new ConcurrentHashMap<>();

    private static final ArrayAdapter<String> asSortedAdapter(Context context, Set<String> set) {
        String[] strings = set.toArray(new String[set.size()]);
        Arrays.sort(strings);
        return new ArrayAdapter<>(
                context,
                R.layout.activity_car_selection_newcar_fueltype_item,
                strings);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.activity_car_selection_newcar_fragment, container, false);
        ButterKnife.bind(this, view);

        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        toolbar.inflateMenu(R.menu.menu_logbook_add_fueling);
        toolbar.setNavigationOnClickListener(v -> {
            hideKeyboard(v);
            closeThisFragment();
        });
        // initially we set the toolbar exp to gone
        toolbar.setVisibility(View.GONE);
        toolbarExp.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);
        downloadView.setVisibility(View.INVISIBLE);

        RxToolbar.itemClicks(toolbar)
                .filter(continueWhenFormIsCorrect())
                .map(createCarFromForm())
                .filter(continueWhenCarHasCorrectValues())
                .map(checkCarAlreadyExist())
                .subscribeWith(new DisposableObserver<Car>() {
                    @Override
                    public void onComplete() {
                        LOG.info("onCompleted car");
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.warn(e.getMessage(), e);
                    }

                    @Override
                    public void onNext(Car car) {
                        LOG.info("car added");
                        ((CarSelectionUiListener) getActivity()).onCarAdded(car);
                        hideKeyboard(getView());
                        closeThisFragment();
                    }
                });

        fueltypeText.setKeyListener(null);

        manufacturerText.setOnItemClickListener((parent, view1, position, id) -> requestNextTextfieldFocus(manufacturerText));
        modelText.setOnItemClickListener((parent, view12, position, id) -> requestNextTextfieldFocus(modelText));
        yearText.setOnItemClickListener((parent, view13, position, id) -> requestNextTextfieldFocus(yearText));
        fueltypeText.setOnItemClickListener((parent, view14, position, id) -> requestNextTextfieldFocus(fueltypeText));

        fetchVehicles();
        addManufacturer();
        initFocusChangedListener();
        initWatcher();
        return view;
    }

    @Override
    public void onResume() {
        LOG.info("onResume()");
        super.onResume();
        ECAnimationUtils.animateShowView(getContext(), toolbar,
                R.anim.translate_slide_in_top_fragment);
        ECAnimationUtils.animateShowView(getContext(), toolbarExp,
                R.anim.translate_slide_in_top_fragment);
        ECAnimationUtils.animateShowView(getContext(), contentView,
                R.anim.translate_slide_in_bottom_fragment);
    }

    @Override
    public void onDestroy() {
        LOG.info("onDestroy()");

        // release all disposables.
        disposables.clear();
        super.onDestroy();
    }

    @OnTextChanged(value = R.id.activity_car_selection_newcar_input_manufacturer, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    protected void onManufacturerChanged(CharSequence text) {
        manufacturerText.setError(null);

        modelText.setText("");
        yearText.setText("");
        engineText.setText("");
        fueltypeText.setText("");
    }

    @OnTextChanged(value = R.id.activity_car_selection_newcar_input_model, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    protected void onModelChanged(CharSequence text) {
        modelText.setError(null);

        yearText.setText("");
        engineText.setText("");
        fueltypeText.setText("");
    }

    @OnTextChanged(value = R.id.activity_car_selection_newcar_input_constructionyear, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    protected void onConstructionYearChanged(CharSequence text) {
        yearText.setError(null);
        engineText.setText("");
        fueltypeText.setText("");
    }

    @OnTextChanged(value = R.id.activity_car_selection_newcar_input_fueltype, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    protected void onFuelTypeChanged(CharSequence text) {
        if (text.toString().isEmpty())
            return;

        Car.FuelType fuelType = Car.FuelType.getFuelTybeByTranslatedString(getContext(), text.toString());

        if (Car.FuelType.ELECTRIC.equals(fuelType)) {
            engineLayout.setVisibility(View.GONE);
            engineText.setVisibility(View.GONE);
        } else {
            engineLayout.setVisibility(View.VISIBLE);
            engineText.setVisibility(View.VISIBLE);
        }
    }

    @OnTextChanged(value = R.id.activity_car_selection_newcar_input_engine)
    protected void onEngineDisplacementChanged(CharSequence text) {
        engineText.setError(null);
    }

    /**
     * Add car button onClick listener. When clicked, it tries to find out if the car already
     * exists. If this is the case, then it adds the car to the list of selected cars. If not,
     * then it selects
     */
    private Predicate<MenuItem> continueWhenFormIsCorrect() {
        return menuItem -> {
            // First, reset the form
            manufacturerText.setError(null);
            modelText.setError(null);
            yearText.setError(null);
            engineText.setError(null);
            fueltypeText.setError(null);

            Car.FuelType fuelType = Car.FuelType.getFuelTybeByTranslatedString(
                    getContext(), fueltypeText.getText().toString());

            //First check all input forms for empty strings
            View focusView = null;
            if (fuelType != Car.FuelType.ELECTRIC && engineText.getText().length() == 0) {
                engineText.setError(getString(R.string.car_selection_error_empty_input));
                focusView = engineText;
            }
            if (fueltypeText.getText().length() == 0) {
                fueltypeText.setError(getString(R.string.car_selection_error_empty_input));
                focusView = fueltypeText;
            }
            if (yearText.getText().length() == 0) {
                yearText.setError(getString(R.string.car_selection_error_empty_input));
                focusView = yearText;
            }
            if (modelText.getText().length() == 0) {
                modelText.setError(getString(R.string.car_selection_error_empty_input));
                focusView = modelText;
            }
            if (manufacturerText.getText().length() == 0) {
                manufacturerText.setError(getString(R.string.car_selection_error_empty_input));
                focusView = manufacturerText;
            }

            // if any of the input forms contained empty values, then set the focus to the
            // last one set.
            if (focusView != null) {
                LOG.info("Some input fields were empty");
                focusView.requestFocus();
                return false;
            } else {
                return true;
            }
        };
    }

    private <T> Function<T, Car> createCarFromForm() {
        return t -> {
            // Get the values
            String manufacturer = manufacturerText.getText().toString();
            String model = modelText.getText().toString();
            String yearString = yearText.getText().toString();
            String engineString = engineText.getText().toString();


            Car.FuelType fueltype = Car.FuelType.getFuelTybeByTranslatedString(getContext(),
                    fueltypeText.getText().toString());

            // create the car
            int year = Integer.parseInt(yearString);
            if (fueltype != Car.FuelType.ELECTRIC) {
                try {
                    int engine = Integer.parseInt(engineString);
                    return new CarImpl(manufacturer, model, fueltype, year, engine);
                } catch (Exception e) {
                    LOG.error(String.format("Unable to parse engine [%s]", engineString), e);
                }
            }
            return new CarImpl(manufacturer, model, fueltype, year);
        };
    }

    private Predicate<Car> continueWhenCarHasCorrectValues() {
        return car -> {
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            View focusView = null;

            // Check the values of engine and year for validity.
            if (car.getFuelType() != Car.FuelType.ELECTRIC &&
                    (car.getEngineDisplacement() < 500 || car.getEngineDisplacement() > 5000)) {
                engineText.setError(getString(R.string.car_selection_error_invalid_input));
                focusView = engineText;
            }
            if (car.getConstructionYear() > currentYear) {
                yearText.setError(getString(R.string.car_selection_error_invalid_input));
                focusView = yearText;
            }

            // if tengine or year have invalid values, then request the focus.
            if (focusView != null) {
                focusView.requestFocus();
                return false;
            }

            return true;
        };
    }

    private void addManufacturer() {
        Single<List<String>> manufacturer = enviroCarVehicleDB.manufacturersDAO().getAllManufacturers();
        manufacturer.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribeWith(new DisposableSingleObserver<List<String>>() {
                    @Override
                    public void onSuccess(List<String> manufacturersNames) {
                        mainThreadWorker.schedule(() -> {
                            if (!manufacturersNames.isEmpty()) {
                                for (int i = 0; i < manufacturersNames.size(); i++) {
                                    mManufacturerNames.add(manufacturersNames.get(i));
                                }
                                updateManufacturerViews();
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error(e.getMessage(), e);
                    }
                });
    }

    private void fetchVehicles() {
        Single<List<Vehicles>> vehicle = enviroCarVehicleDB.vehicleDAO().getManufacturerVehicles();
        vehicle.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribeWith(new DisposableSingleObserver<List<Vehicles>>() {
                    @Override
                    public void onSuccess(List<Vehicles> vehicles) {
                        for (Vehicles vehicles1 : vehicles) {
                            Car car = new CarImpl();
                            car.setManufacturer(vehicles1.getManufacturer());
                            car.setModel(vehicles1.getCommerical_name());
                            String date = vehicles1.getAllotment_date();
                            int allotmentDate = convertDateToInt(date);
                            car.setConstructionYear(allotmentDate);
                            car.setFuelType(getFuel(vehicles1.getPower_source_id()));
                            if (!vehicles1.getEngine_capacity().isEmpty() && vehicles1.getEngine_capacity().matches("[0-9]+"))
                                car.setEngineDisplacement(Integer.parseInt(vehicles1.getEngine_capacity()));
                            addCarToAutocompleteList(car);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error(e.getMessage(), e);
                    }
                });
    }

    private Car.FuelType getFuel(String id) {
        String fuel = null;
        if (id.equals("01"))
            fuel = "gasoline";
        else if (id.equals("02"))
            fuel = "diesel";
        else if (id.equals("04"))
            fuel = "electric";
        else if (id.equals("05") || id.equals("09") || id.equals("38"))
            fuel = "gas";
        else
            fuel = "hybrid";

        return Car.FuelType.resolveFuelType(fuel);
    }

    private int convertDateToInt(String date) {
        int convertedDate = 0;
        for (int i = 6; i < date.length(); i++) {
            convertedDate = convertedDate * 10 + (date.charAt(i) - 48);
        }
        return convertedDate;
    }

    private Function<Car, Car> checkCarAlreadyExist() {
        return car -> {
            String manu = car.getManufacturer();
            String model = car.getModel();
            String year = "" + car.getConstructionYear();
            String engine = "" + car.getEngineDisplacement();
            Pair<String, String> modelYear = new Pair<>(model, year);

            Car selectedCar = null;
            if (mManufacturerNames.contains(manu)
                    && mCarToModelMap.get(manu) != null
                    && mCarToModelMap.get(manu).contains(model)
                    && mModelToYear.get(model) != null
                    && mModelToYear.get(model).contains(year)
                    && mModelToCCM.get(modelYear) != null
                    && mModelToCCM.get(modelYear).contains(engine)) {
                for (Car other : mCars) {
                    if (other.getManufacturer().equals(manu)
                            && other.getModel().equals(model)
                            && other.getConstructionYear() == car.getConstructionYear()
                            && other.getEngineDisplacement() == car.getEngineDisplacement()
                            && other.getFuelType() == car.getFuelType()) {
                        selectedCar = other;
                        break;
                    }
                }
            }

            if (selectedCar == null) {
                LOG.info("New Car type. Register car at server.");
                carManager.registerCarAtServer(car);
                return car;
            } else {
                LOG.info(String.format("Car already existed -> [%s]", selectedCar.getId()));
                return selectedCar;
            }
        };
    }

    private void initFocusChangedListener() {
        manufacturerText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String manufacturer = manufacturerText.getText().toString();
                updateModelViews(manufacturer);
            }
        });

        modelText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String model = modelText.getText().toString();
                updateYearView(model);
            }
        });

        yearText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String year = yearText.getText().toString();
                String model = modelText.getText().toString();
                Pair<String, String> modelYear = new Pair<>(model, year);

                updateFuelView(modelYear);
            }
        });

        fueltypeText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String year = yearText.getText().toString();
                String model = modelText.getText().toString();
                Pair<String, String> modelYear = new Pair<>(model, year);

                updateEngineView(modelYear);
            }
        });
    }

    private void checkFuelingType() {
        String manufacturer = manufacturerText.getText().toString();
        String model = modelText.getText().toString();
        String yearString = yearText.getText().toString();
        String engineString = engineText.getText().toString();
        Pair<String, String> modelYear = new Pair<>(model, yearString);

        Car selectedCar = null;
        if (mManufacturerNames.contains(manufacturer)
                && mCarToModelMap.get(manufacturer) != null
                && mCarToModelMap.get(manufacturer).contains(model)
                && mModelToYear.get(model) != null
                && mModelToYear.get(model).contains(yearString)
                && mModelToCCM.get(modelYear) != null
                && mModelToCCM.get(modelYear).contains(engineString)) {
            for (Car other : mCars) {
                if (other.getManufacturer() == null ||
                        other.getModel() == null ||
                        other.getConstructionYear() == 0 ||
                        other.getEngineDisplacement() == 0 ||
                        other.getFuelType() == null) {
                    continue;
                }
                if (other.getManufacturer().equals(manufacturer)
                        && other.getModel().equals(model)
                        && other.getConstructionYear() == Integer.parseInt(yearString)
                        && other.getEngineDisplacement() == Integer.parseInt(engineString)) {
                    selectedCar = other;
                    break;
                }
            }
        }

    }

    private void updateManufacturerViews() {
        if (!mManufacturerNames.isEmpty()) {
            manufacturerText.setAdapter(asSortedAdapter(getContext(), mManufacturerNames));
        } else {
            manufacturerText.setAdapter(null);
        }
    }

    private void updateModelViews(String manufacturer) {
        if (mCarToModelMap.containsKey(manufacturer)) {
            modelText.setAdapter(asSortedAdapter(getContext(), mCarToModelMap.get(manufacturer)));
        } else {
            modelText.setAdapter(null);
        }
    }

    private void updateYearView(String model) {
        if (mModelToYear.containsKey(model)) {
            yearText.setAdapter(asSortedAdapter(getContext(), mModelToYear.get(model)));
        } else {
            yearText.setAdapter(null);
        }
    }

    private void updateEngineView(Pair<String, String> model) {
        if (mModelToCCM.containsKey(model)) {
            engineText.setAdapter(asSortedAdapter(getContext(), mModelToCCM.get(model)));
        } else {
            engineText.setAdapter(null);
        }
    }

    private void updateFuelView(Pair<String, String> model) {
        if (mModelYearToFuel.containsKey(model)) {
            fueltypeText.setAdapter(asSortedAdapter(getContext(), mModelYearToFuel.get(model)));
        } else {
            fueltypeText.setAdapter(null);
        }
    }

    /**
     * Inserts the attributes of the car
     *
     * @param car
     */
    private void addCarToAutocompleteList(Car car) {

        //mCars.add(car);
        String manufacturer = car.getManufacturer().trim();
        String model = car.getModel().trim();
        String year = Integer.toString(car.getConstructionYear());
        Car.FuelType fuelType = car.getFuelType();
        String fuel = getContext().getString(fuelType.getStringResource());

        if (manufacturer.isEmpty() || model.isEmpty() || year.isEmpty())
            return;


        if (!mCarToModelMap.containsKey(manufacturer))
            mCarToModelMap.put(manufacturer, new HashSet<>());
        mCarToModelMap.get(manufacturer).add(model);

        if (!mModelToYear.containsKey(model))
            mModelToYear.put(model, new HashSet<>());
        mModelToYear.get(model).add(Integer.toString(car.getConstructionYear()));

        Pair<String, String> modelYearPair = new Pair<>(model, year);
        if (!mModelYearToFuel.containsKey(modelYearPair))
            mModelYearToFuel.put(modelYearPair, new HashSet<>());
        mModelYearToFuel.get(modelYearPair).add(fuel);

        if (!mModelToCCM.containsKey(modelYearPair))
            mModelToCCM.put(modelYearPair, new HashSet<>());
        mModelToCCM.get(modelYearPair).add(Integer.toString(car.getEngineDisplacement()));
    }

    public void closeThisFragment() {
        // ^^
        ECAnimationUtils.animateHideView(getContext(),
                ((CarSelectionActivity) getActivity()).overlayView, R.anim.fade_out);
        ECAnimationUtils.animateHideView(getContext(), R.anim
                .translate_slide_out_top_fragment, toolbar, toolbarExp);
        ECAnimationUtils.animateHideView(getContext(), contentView, R.anim
                .translate_slide_out_bottom, () -> ((CarSelectionUiListener) getActivity()).onHideAddCarFragment());
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent appComponent) {
        appComponent.inject(this);
    }

    private void initWatcher() {
        disposables.add(RxTextView.textChanges(manufacturerText)
                .skipInitialValue()
                .debounce(ERROR_DEBOUNCE_TIME, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(t -> t.toString())
                .subscribe(manufacturer -> {
                    ListAdapter adapter = manufacturerText.getAdapter();
                    int flag = 0;
                    for (int i = 0; i < adapter.getCount(); i++) {
                        if (adapter.getItem(i).toString().compareTo(manufacturer) == 0) {
                            flag = 1;
                            break;
                        }
                    }
                    if (flag == 0) {
                        manufacturerText.setError(getString(R.string.car_selection_error_select_from_list));
                        manufacturerText.requestFocus();
                    }
                }));
        disposables.add(RxTextView.textChanges(modelText)
                .skipInitialValue()
                .debounce(ERROR_DEBOUNCE_TIME, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(t -> t.toString())
                .subscribe(model -> {
                    if (model.trim().isEmpty()) {
                        modelText.setError(getString(R.string.car_selection_error_empty_input));
                    } else {
                        ListAdapter adapter = modelText.getAdapter();
                        int flag = 0;
                        for (int i = 0; i < adapter.getCount(); i++) {
                            if (adapter.getItem(i).toString().compareTo(model) == 0) {
                                flag = 1;
                                break;
                            }
                        }
                        if (flag == 0) {
                            modelText.setError(getString(R.string.car_selection_error_select_from_list));
                            modelText.requestFocus();
                        }
                    }
                }, LOG::error));

        // Year input validity check.
        disposables.add(RxTextView.textChanges(yearText)
                .skipInitialValue()
                .debounce(ERROR_DEBOUNCE_TIME, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(CharSequence::toString)
                .filter(s -> !s.isEmpty())
                .subscribe(yearString -> {
                    ListAdapter adapter = yearText.getAdapter();
                    int flag = 0;
                    for (int i = 0; i < adapter.getCount(); i++) {
                        if (adapter.getItem(i).toString().compareTo(yearString) == 0) {
                            flag = 1;
                            break;
                        }
                    }
                    if (flag == 0) {
                        yearText.setError(getString(R.string.car_selection_error_select_from_list));
                        yearText.requestFocus();
                    }
                }, LOG::error));

        // Engine input validity check.
        disposables.add(RxTextView.textChanges(engineText)
                .skipInitialValue()
                .debounce(ERROR_DEBOUNCE_TIME, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(CharSequence::toString)
                .filter(s -> !s.isEmpty())
                .subscribe(engineString -> {
                    if (engineString.isEmpty())
                        return;
                    ListAdapter adapter = engineText.getAdapter();
                    int flag = 0;
                    for (int i = 0; i < adapter.getCount(); i++) {
                        if (adapter.getItem(i).toString().compareTo(engineString) == 0) {
                            flag = 1;
                            break;
                        }
                    }
                    if (flag == 0) {
                        engineText.setError(getString(R.string.car_selection_error_select_from_list));
                        engineText.requestFocus();
                    }
                }, LOG::error));
    }

    private void requestNextTextfieldFocus(TextView textField) {
        try {
            TextView nextField = (TextView) textField.focusSearch(View.FOCUS_DOWN);
            nextField.requestFocus();
        } catch (Exception e) {
            LOG.warn("Unable to find next field or to request focus to next field.");
        }
    }
}
