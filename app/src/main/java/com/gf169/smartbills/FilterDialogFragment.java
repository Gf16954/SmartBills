package com.gf169.smartbills;

import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static android.text.InputType.TYPE_CLASS_DATETIME;
import static android.text.InputType.TYPE_CLASS_NUMBER;
import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_DATETIME_VARIATION_DATE;
import static android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;
import static com.gf169.smartbills.Common.ATTR_TYPE_ASSOCIATION;
import static com.gf169.smartbills.Common.ATTR_TYPE_DATA;
import static com.gf169.smartbills.Common.ATTR_TYPE_ENUM;
import static com.gf169.smartbills.Common.EntityField;
import static com.gf169.smartbills.Common.EnumItem;
import static com.gf169.smartbills.Common.SDF_OUR;
import static com.gf169.smartbills.Common.SpinnerContainer;
import static com.gf169.smartbills.Common.TYPE_BOOLEAN;
import static com.gf169.smartbills.Common.TYPE_DATE;
import static com.gf169.smartbills.Common.TYPE_DATETIME;
import static com.gf169.smartbills.Common.TYPE_DECIMAL;
import static com.gf169.smartbills.Common.TYPE_INTEGER;
import static com.gf169.smartbills.Common.TYPE_STRING;
import static com.gf169.smartbills.Common.TYPE_UUID;
import static com.gf169.smartbills.Common.VALUE_NO;
import static com.gf169.smartbills.Common.VALUE_YES;
import static com.gf169.smartbills.Common.formEntityFieldsArray;
import static com.gf169.smartbills.Common.mainEntityClass;
import static com.gf169.smartbills.ESMisc.mainEntityName;
import static com.gf169.smartbills.Utils.message;

//public class FilterDialogFragment extends AppCompatDialogFragment implements View.OnClickListener {
public class FilterDialogFragment extends DialogFragment implements View.OnClickListener {
    static final String TAG = "gfFilterDialogFragment";

    static final String OPERATOR_SET = "Установлен";
    static final String OPERATOR_NOT_SET = "НЕ установлен";

    View view;  // корневой этого фрагмента

    static ArrayList<EntityField> mainEntityFields; // Все поля этой сущности
    ListView filterItemList;
    static ArrayList<FilterItem> filterItems;  // static чтобы не сбрасывался при выходе и последующем входе
    // А также сохраняется при перевороте! Можно не делать onSaveInstanceState !
    static boolean toApplyFilter;              // Флаг MainActivity
    static ArrayList<EntityField> entityFields;      // static чтобы не сбрасывался при выходе и последующем входе

    EditText etVCur;
    int curPos;
    FilterItem filterItemTemp = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        // В стиле сказано, что должен быть заголовок, в семерке по умолчанию нет.
        // Должно быть именно здесь, иначе не работает
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogFragmentStyle);
    }

    /*
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Log.d(TAG, "onCreateDialog");
            super.onCreate(savedInstanceState);
    */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        TextView tv = getDialog().findViewById(android.R.id.title);
        tv.getLayoutParams().height =
                (int) getResources().getDimension(R.dimen.filter_title_height);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(R.dimen.filter_title_text_size));

        if (Build.VERSION.SDK_INT >= 17) {
            tv.setPadding(0, 0, 0, 0);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        } else {
            tv.setPadding(20, 0, 0, 0);
        }
        getDialog().setTitle("Условия поиска");

        view = inflater.inflate(R.layout.fragment_filter, null);
//        adjustFragmentSize(view,0.5F, 0.9F);
// TODO: 31.03.2019 Заставить работать правильно         

        view.findViewById(R.id.buttonApplyFilter).setOnClickListener(this);
        view.findViewById(R.id.buttonClearFilter).setOnClickListener(this);

        showList();

        return view;
    }

    @Override
    public void onClick(View v) {
        if (v == v.findViewById(R.id.buttonApplyFilter)) {
            if (!saveFilterItem(true))
                return;   // Последний добавляем сами, если хороший (юзер забыл нажать +)
            try {  // Дозаполняем для reflection
                for (int i = 0; i < filterItems.size() - 1; i++) {
                    if (filterItems.get(i).entityField.reflectField == null) {  // Еще не заполнено
                        java.lang.reflect.Field reflectField =
                                mainEntityClass.getDeclaredField(filterItems.get(i).entityField.name);
                        reflectField.setAccessible(true);

                        EntityField entityField = filterItems.get(i).entityField;
                        entityFields.get(entityFields.indexOf(entityField)).reflectField = reflectField;
                        entityField.reflectField = reflectField;
                    }
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();  // Не может быть
            }
            toApplyFilter = filterItems.size() > 1;   // Фильтровать, если не пустой (первый эл-т - пустой для ввода)
        } else if (v == v.findViewById(R.id.buttonClearFilter)) {
            toApplyFilter = false;  // Показывать нефильтрованный список
        }

        dismiss();  // Вернемся сразу в Main
        ((MainActivity) getActivity()).showList(true,  // Показываем список
                ((MainActivity) getActivity()).dataset.getSelectedItemId(),  // Хотим сохранить выделение
//                ((MainActivity) getActivity()).dataset.getMarkedItemIdsString()); // и отметки
                null);
    }

    @Override
    public void onCancel(DialogInterface dialog) {  // При возврате в Main по кнопке Назад
        super.onCancel(dialog);
    }

    class FilterItemsArrayAdapter extends ArrayAdapter<FilterItem> {
        FilterItemsArrayAdapter(Context context, ArrayList<FilterItem> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View itemView, ViewGroup parent) {
            Log.d(TAG, "getView " + position);

            if (itemView == null) {
                itemView = LayoutInflater.from(getContext()).inflate(R.layout.filter_item, parent,
                        false);
            }

            SpinnerContainer scF[] = {null};
            SpinnerContainer scO[] = {null};
            SpinnerContainer scV[] = {null};
            EditText etV[] = {null};

            FilterItem filterItem = getItem(position);

            scF[0] = new SpinnerContainer(
                    getActivity(),
                    itemView.findViewById(R.id.fieldSpinner),
                    // (ArrayList<Object>)entityFields,  Не может конвертировать! Почему?
                    // (ArrayList<? extends Object>)entityFields, // Не может конвертировать! Почему?
                    new ArrayList<>(entityFields),
                    filterItem.entityField,
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            Log.d(TAG, "onItemSelected override scF.selectedItem " + scF[0].selectedItem);

                            scF[0].defaultOnItemSelectedListener.onItemSelected(adapterView, view, i, l);

                            if (position != curPos) {  // Перешли в эту строку
                                curPos = position;
                                filterItemTemp = filterItem.clone();
                                etVCur = etV[0];
                            }
                            if (scF[0].selectedItem.toString() != null) {  // Не первый пустой элемент; toString переопределено!
                                EntityField entityField = (EntityField) scF[0].selectedItem;
                                if (!entityField.equals(filterItemTemp.entityField)) {  // Выбрали новое значение
                                    filterItemTemp.entityField = entityField;

                                    SpinnerContainer s = scO[0];
                                    filterItemTemp.possibleOperators = filterItemTemp.formOperatorsArray();
                                    s.items.clear();
                                    s.items.addAll(filterItemTemp.possibleOperators);
                                    ((ArrayAdapter) s.spinner.getAdapter()).notifyDataSetChanged();

                                    s = scV[0];
                                    EditText e = etV[0];
                                    if (entityField.spinnerNeeded()) {
                                        filterItemTemp.entityField.possibleValues =
                                                filterItemTemp.entityField.formReferencesArray(false);
                                        s.items.clear();
                                        s.items.addAll(filterItemTemp.entityField.possibleValues);
                                        ((ArrayAdapter) s.spinner.getAdapter()).notifyDataSetChanged();
                                        s.spinner.setVisibility(View.VISIBLE);
                                        e.setVisibility(View.INVISIBLE);
                                    } else {
                                        s.spinner.setVisibility(View.INVISIBLE);
                                        e.setVisibility(View.VISIBLE);
                                        e.setInputType(
                                                entityField.type.equals(TYPE_DATE) ||
                                                        entityField.type.equals(TYPE_DATETIME) ? TYPE_CLASS_DATETIME | TYPE_DATETIME_VARIATION_DATE :
                                                        entityField.type.equals(TYPE_INTEGER) ? TYPE_CLASS_NUMBER :
                                                                entityField.type.equals(TYPE_DECIMAL) ? TYPE_CLASS_NUMBER | TYPE_NUMBER_FLAG_DECIMAL :
                                                                        TYPE_CLASS_TEXT);
                                        e.setText(null);
                                    }
                                }
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {
                        }
                    });
            scF[0].spinner.setEnabled(filterItem.isNew);

            scO[0] = new SpinnerContainer(
                    getActivity(),
                    itemView.findViewById(R.id.operatorSpinner),
                    filterItem.isNew ? null : filterItem.possibleOperators,
                    filterItem.isNew ? null : filterItem.operator,
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            Log.d(TAG, "onItemSelected override scO.selectedItem " + scO[0].selectedItem);
                            scO[0].defaultOnItemSelectedListener.onItemSelected(adapterView, view, i, l);

                            filterItemTemp.operator = (String) scO[0].selectedItem;

                            if (scO[0].selectedItem == OPERATOR_SET ||
                                    scO[0].selectedItem == OPERATOR_NOT_SET) {
                                scV[0].spinner.setVisibility(View.INVISIBLE);
                                etV[0].setVisibility(View.INVISIBLE);
                            } else {
                                if (((EntityField) scF[0].selectedItem).spinnerNeeded()) {
                                    scV[0].spinner.setVisibility(View.VISIBLE);
                                    etV[0].setVisibility(View.INVISIBLE);
                                } else {
                                    scV[0].spinner.setVisibility(View.INVISIBLE);
                                    etV[0].setVisibility(View.VISIBLE);
                                }
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {
                        }
                    });
            scO[0].spinner.setEnabled(filterItem.isNew);

            scV[0] = new SpinnerContainer(  // Пока невидим
                    getActivity(),
                    itemView.findViewById(R.id.valueSpinner),
                    filterItem.isNew ? null : filterItem.entityField.possibleValues,
                    filterItem.isNew || filterItem.entityField == null ||
                            !filterItem.entityField.spinnerNeeded() ? null : filterItem.value,
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            Log.d(TAG, "onItemSelected override scV.selectedItem " + scV[0].selectedItem);
                            scV[0].defaultOnItemSelectedListener.onItemSelected(adapterView, view, i, l);

                            filterItemTemp.value = scV[0].selectedItem;
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {
                        }
                    });
            scV[0].spinner.setEnabled(filterItem.isNew);
            scV[0].spinner.setVisibility(View.INVISIBLE);  // Пока невидим

            etV[0] = itemView.findViewById(R.id.valueEditText);
            etV[0].setText(filterItem.entityField == null || filterItem.value == null ||
                    filterItem.entityField.spinnerNeeded() ? null : filterItem.value.toString());
            etV[0].setEnabled(filterItem.isNew);
            etV[0].setVisibility(View.INVISIBLE);   // Пока невидим

            Button button = itemView.findViewById(R.id.buttonAddRemove);
            button.setText(filterItem.isNew ? "+" : "X");
            button.setOnClickListener((view) -> {
                if (((Button) view).getText().equals("+")) {  // Сохраняем в массив
                    saveFilterItem(false);
                } else { // "-"
                    filterItems.remove(position);
                    ((ArrayAdapter<Object>) filterItemList.getAdapter()).notifyDataSetChanged();
                }
            });

            return itemView;
        }
    }

    boolean saveFilterItem(boolean isAutomatic) {
        if (filterItemTemp == null ||  // Ничего не ввели в последнюю строку - игнорируем ее
                filterItemTemp.entityField == null ||
                filterItemTemp.entityField.description == null) { // Или очистили
            return true;
        }
        if (etVCur.getVisibility() == View.VISIBLE) {
            filterItemTemp.value = etVCur.getText().toString();
        }
        if (!filterItemTemp.control()) {
            if (isAutomatic) { // При автоматическом сохранении просто проигнорируем недовведенный
                return true;
            } else {
                message("Ошибка при вводе параметров поиска\n" + filterItemTemp.error);
                return false;
            }
        }
        if (filterItemTemp.control2()) {
            filterItemTemp.isNew = false;
            filterItems.add(filterItems.size() - 1, filterItemTemp.clone()); // Предпоследний !
            ((ArrayAdapter<Object>) filterItemList.getAdapter()).notifyDataSetChanged();
            return true;
        } else {
            message("Ошибка при вводе параметров поиска\n" + filterItemTemp.error);
        }
        return false;
    }

    void showList() {
        Log.d(TAG, "showFilterItemList");

        if (filterItems == null) {
            filterItems = new ArrayList<>(
                    Arrays.asList(new FilterItem(null, null, null, true)));  // Пустой для ввода
            if (mainEntityFields == null) {
                mainEntityFields = formEntityFieldsArray(mainEntityName);
            }
            if (entityFields == null) {
                entityFields = new ArrayList<>(mainEntityFields);
                entityFields.add(0, new EntityField(null, null, null, null)); // Пустой в начало
            }
        }
        ArrayAdapter<FilterItem> adapter = new FilterItemsArrayAdapter(getActivity(), filterItems);
        filterItemList = view.findViewById(R.id.filterItems);
        filterItemList.setAdapter(adapter);
        curPos = -1;
    }

    static class FilterItem {
        EntityField entityField;

        ArrayList<Object> possibleOperators;
        String operator;

        Object value;

        boolean isNew;   // Последняя, добавляемая строка
        String error;

        public FilterItem(EntityField entityField, String operator, Object value, boolean isNew) {
            //                  ArrayList<Object> possibleOperators, ArrayList<Object> possibleValues)
            this.entityField = entityField;
            this.operator = operator;
            this.value = value;
            this.isNew = isNew;
//            this.possibleOperators = possibleOperators;
//            this.possibleValues = possibleValues;
        }

        public FilterItem clone() {
            FilterItem filterItem = new FilterItem(entityField, operator, value, isNew);
            filterItem.possibleOperators = possibleOperators;
            return filterItem;
        }

        ArrayList<Object> formOperatorsArray() { // String
            if (entityField == null || entityField.attributeType == null || entityField.type == null)
                return null;

            ArrayList<Object> r = new ArrayList<>();

            if (entityField.attributeType.equals(ATTR_TYPE_ASSOCIATION) &&
                    !entityField.isCollection()) {  // Ссылка
//            r = new ArrayList<>(java.util.Arrays.asList("=", "<>", "содержит", "НЕ содержит",
//                    "начинается с", "заканчивается на"));
                r = new ArrayList<>(java.util.Arrays.asList("=", "<>"));
            }
            if (entityField.attributeType.equals(ATTR_TYPE_DATA)) {
                switch (entityField.type) {
                    case TYPE_BOOLEAN:
                        r = new ArrayList<>(java.util.Arrays.asList("="));
                        break;
                    case TYPE_INTEGER:
                    case TYPE_DECIMAL:
                    case TYPE_DATE:
                    case TYPE_DATETIME:
                        r = new ArrayList<>(java.util.Arrays.asList("=", "<", "<=", ">", ">=", "<>"));
                        break;
                    case TYPE_STRING:  // ToDo Добавить учет регистра?
                        r = new ArrayList<>(java.util.Arrays.asList("=", "<>", "содержит", "НЕ содержит",
                                "начинается с", "заканчивается на"));
                        break;
                    case TYPE_UUID:  // ToDo добавить ?
                        break;
                }
            }
            if (entityField.attributeType.equals(ATTR_TYPE_ENUM)) {
                r = new ArrayList<>(java.util.Arrays.asList("=", "<>"));
            }

            r.add(OPERATOR_SET);
            r.add(OPERATOR_NOT_SET);
            return r;
        }

        boolean test(Object entity) {
            Log.d(TAG, "test " + entityField + " " + operator + " " + value);

            try {
                Object o = entityField.reflectField.get(entity);

                if (operator.equals(OPERATOR_SET)) {
                    return !entityField.isCollection() && o != null ||
                            entityField.isCollection() && o != null && !((ArrayList<Object>) o).isEmpty();
                }
                if (operator.equals(OPERATOR_NOT_SET)) {
                    return !entityField.isCollection() && o == null ||
                            entityField.isCollection() && (o == null || ((ArrayList<Object>) o).isEmpty());
                }
                if (o == null) return false; // null ни к чему не подходит

                String filterValueStr = value.toString();

                if (entityField.attributeType.equals(ATTR_TYPE_DATA)) {
                    switch (entityField.type) {
                        case TYPE_BOOLEAN:
                            if ((Boolean) o && filterValueStr.equals(VALUE_NO) ||
                                    !(Boolean) o && filterValueStr.equals(VALUE_YES)
                            ) return false;
                            break;
                        case TYPE_INTEGER:
                            if (!testInt((Integer) o, operator, filterValueStr)) return false;
                            break;
                        case TYPE_DECIMAL:
                            if (!testDouble((Double) o, operator, filterValueStr)) return false;
                            break;
                        case TYPE_DATE:
                        case TYPE_DATETIME:
                            if (!testDate((Date) o, operator, filterValueStr)) return false;
                            break;
                        case TYPE_UUID:  // ToDo
                            break;
                        case TYPE_STRING:
                            if (!testString((String) o, operator, filterValueStr)) return false;
                            break;
                    }
                } else if (entityField.attributeType.equals(ATTR_TYPE_ASSOCIATION)) {  // ToDo Оптимизировать
                    Class referencedEntityClass = entityField.getReferencedClass();
/*
                    Field referencedEntityField = referencedEntityClass.getDeclaredField("_instanceName");
                    referencedEntityField.setAccessible(true);
                    String referencedEntityFieldValue = (String) referencedEntityField.get(o);
                    if (!testString(referencedEntityFieldValue, operator, filterValueStr))
                        return false;
К очень большому сожалению не у всех entities заполнено поле _instanceName :(
*/
                    Method referencedEntityMethod = referencedEntityClass.getMethod(
                            "getInstanceName", (Class[]) null);
                    referencedEntityMethod.setAccessible(true);
                    String referencedEntityMethodResult = (String) referencedEntityMethod.invoke(o);
                    if (!testString(referencedEntityMethodResult, operator, filterValueStr))
                        return false;
                } else if (entityField.attributeType.equals(ATTR_TYPE_ENUM)) {
                    // В json'e поля типа отражается так: "type_payment"="CASH", значение - не объект, строка с полем Enum'a name
                    if (!testString((String) o, operator, ((EnumItem) value).name))
                        return false;  // name!
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        boolean testInt(int i, String operator, String value) {
            int j = Integer.decode(value);
            return "=".equals(operator) && i == j ||
                    "<".equals(operator) && i < j ||
                    "<=".equals(operator) && i <= j ||
                    ">".equals(operator) && i > j ||
                    ">=".equals(operator) && i >= j ||
                    "<>".equals(operator) && !(i == j);
        }

        boolean testDouble(double i, String operator, String value) {
            double j = Double.parseDouble(value);
            return "=".equals(operator) && i == j ||
                    "<".equals(operator) && i < j ||
                    "<=".equals(operator) && i <= j ||
                    ">".equals(operator) && i > j ||
                    ">=".equals(operator) && i >= j ||
                    "<>".equals(operator) && !(i == j);
        }

        boolean testDate(Date d, String operator, String value) {
            if (d == null) return false;

            int j = d.compareTo(Utils.dateStr2Date(value, SDF_OUR));
            return testInt(j, operator, "0");
        }

        boolean testString(String s, String operator, String value) {   // ToDo Добавить учет регистра?
            if (s == null) return false;

            s = s.toLowerCase();
            value = value.toLowerCase();

            return "=".equals(operator) && value.equals(s) ||
                    "<>".equals(operator) && !value.equals(s) ||
                    "содержит".equals(operator) && s.contains(value) ||
                    "НЕ содержит".equals(operator) && !s.contains(value) ||
                    "начинается с".equals(operator) && !s.startsWith(value) ||
                    "кончается на".equals(operator) && !s.endsWith(value);
        }

        boolean control() {
            error = null;
            if (entityField == null || entityField.description == null ||
                    operator == null ||
                    (value == null || value.toString().isEmpty())
                            && !operator.equals(OPERATOR_SET) && !operator.equals(OPERATOR_NOT_SET))
                error = "Обязательные поля не введены";
            return error == null;
        }

        boolean control2() {
            error = null;
/*
            if (test(new Entities.Query(true), true) == true) {
                error = operator + " " + value;
            }
*/
            return error == null;
        }
    }

    static boolean testEntity(Object entity) {
        Log.d(TAG, "testEntity " + (filterItems.size() - 1) + " " + entity);

        for (int i = 0; i < filterItems.size() - 1; i++) {
            if (!filterItems.get(i).test(entity)) return false;
        }
        return true;
    }
}
