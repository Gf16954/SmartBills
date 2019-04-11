package com.gf169.smartbills;

import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static android.text.InputType.TYPE_CLASS_DATETIME;
import static android.text.InputType.TYPE_CLASS_NUMBER;
import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_DATETIME_VARIATION_DATE;
import static android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;
import static com.gf169.smartbills.Common.ATTR_TYPE_ASSOCIATION;
import static com.gf169.smartbills.Common.ATTR_TYPE_DATA;
import static com.gf169.smartbills.Common.ATTR_TYPE_ENUM;
import static com.gf169.smartbills.Common.EDITABILITY_EDITABLE;
import static com.gf169.smartbills.Common.EDITABILITY_MANDATORY;
import static com.gf169.smartbills.Common.EDITABILITY_NOT_EDITABLE;
import static com.gf169.smartbills.Common.EntityField;
import static com.gf169.smartbills.Common.EnumItem;
import static com.gf169.smartbills.Common.Get;
import static com.gf169.smartbills.Common.GetWorkflow;
import static com.gf169.smartbills.Common.OM;
import static com.gf169.smartbills.Common.Reference;
import static com.gf169.smartbills.Common.SDF_OUR;
import static com.gf169.smartbills.Common.SDF_SERVER;
import static com.gf169.smartbills.Common.SpinnerContainer;
import static com.gf169.smartbills.Common.TYPE_BOOLEAN;
import static com.gf169.smartbills.Common.TYPE_DATE;
import static com.gf169.smartbills.Common.TYPE_DATETIME;
import static com.gf169.smartbills.Common.TYPE_DECIMAL;
import static com.gf169.smartbills.Common.TYPE_INTEGER;
import static com.gf169.smartbills.Common.VALUE_NO;
import static com.gf169.smartbills.Common.VALUE_YES;
import static com.gf169.smartbills.Common.cr;
import static com.gf169.smartbills.Common.curActivity;
import static com.gf169.smartbills.Common.formEntityFieldsArray;
import static com.gf169.smartbills.ESMisc.mainEntityName;

public class EditDialogFragment extends DialogFragment
        implements View.OnClickListener, View.OnFocusChangeListener {
    static final String TAG = "gfEditDialogFragment";

    static final String EDIT_MODE_VIEW = "0";
    static final String EDIT_MODE_EDIT = "1";
    static final String EDIT_MODE_CREATION = "2";

    static boolean isFragmentNewIstance;    // true - запуск фрагмента из программы,
    // а не автоматическое пересоздание при перевороте
    // ToDo сохранение/восстановление

    static ArrayList<EntityField> mainEntityFields; // Все поля этой сущности
    static String curStepName;
    static ArrayList<EntityField> entityFields; // Поля этой сущности с установленными editability и отсортированные

    View view;  // корневой этого фрагмента
    ListView fieldListView;  // главный ListView
    View curFocusedView; // view текущего itme'a

    public static class EditParameters {
        String editMode;
        String entityName;
        String stepName;

        Get objIn;
        Get objOut;

        public EditParameters(String editMode, String entityName, Get objIn, Get objOut) {
            this.editMode = editMode;
            this.entityName = entityName;
            this.stepName = ((GetWorkflow) objIn).getStepName();
            this.objIn = objIn;
            this.objOut = objOut;
        }

        int control(ArrayList<EntityField> entityFields) {
            String s = "";
            int i = -1;
            EntityField ef;
            for (int j = 0; j < entityFields.size(); j++) {
                ef = entityFields.get(j);
                if (ef.editability.equals(EDITABILITY_MANDATORY) && (ef.value == null ||
                        ef.value.toString() == null || ef.value.toString().trim().isEmpty())) {  // toString переопределено!
                    s += "Не заполнено поле " + ef.description + "\n";
                    if (i < 0) {
                        i = j;
                    }
                }
            }
            if (i >= 0) {
                Utils.message(s);
            }
            return i;
        }
    }

    static EditParameters ep;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        // В стиле сказано, что должен быть заголовок, в семерке по умолчанию нет.
        // Должно быть именно здесь, иначе не работает
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogFragmentStyle);
    }

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

        view = inflater.inflate(R.layout.fragment_edit, container);
//        adjustFragmentSize(view,1F, 1F);  // TODO: 31.03.2019 Разобраться и заставить работать!!! 

        showList();

        if (ep.editMode.equals(EDIT_MODE_VIEW)) {
            view.findViewById(R.id.buttonOK).setVisibility(View.INVISIBLE);
            view.findViewById(R.id.buttonCancel).setVisibility(View.INVISIBLE);
            view.findViewById(R.id.buttonActions).setVisibility(View.VISIBLE);
            view.findViewById(R.id.buttonActions).setOnClickListener(this);
        } else {
            view.findViewById(R.id.buttonOK).setVisibility(View.VISIBLE);
            view.findViewById(R.id.buttonCancel).setVisibility(View.VISIBLE);
            view.findViewById(R.id.buttonActions).setVisibility(View.INVISIBLE);
            view.findViewById(R.id.buttonOK).setOnClickListener(this);
            view.findViewById(R.id.buttonCancel).setOnClickListener(this);
        }
        return view;
    }

    @Override
    public void onResume() {  // ToDo
        Log.d(TAG, "onResume");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 25 = 7
//        int width = 1500; //ConstraintLayout.LayoutParams.MATCH_PARENT;
//        int height = 1500; //ConstraintLayout.LayoutParams.MATCH_PARENT;
            int width = getResources().getDisplayMetrics().widthPixels; // Не меньше!
            int height = 1500; // ToDo
            getDialog().getWindow().setLayout(width, height);
        } else {
            getDialog().findViewById(android.R.id.title).getLayoutParams().height =
                    com.gf169.gfutils.Utils.dpyToPx(20);
            ((TextView) getDialog().findViewById(android.R.id.title)).
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        }
        super.onResume();
    }

    @Override
    public void onClick(View v) {  // Button
        if (v == v.findViewById(R.id.buttonOK)) {

            storeFieldValue(curFocusedView);

            int iErPos = ep.control(entityFields);
            if (iErPos >= 0) {
                return;    // ToDo Встать на ошибочное поле
            }
            if (send()) {
                curActivity.showList(     // Возвращаемся в главный список
                        false, ep.objOut.getId(), // Созданную выделяем
//                        ((MainActivity) getActivity()).dataset.getMarkedItemIdsString());
                        null);
            }
            dismiss();

        } else if (v == v.findViewById(R.id.buttonCancel)) {
            dismiss();

        } else {  // Actions
            curActivity.showActionsPopup(curActivity.dataset.getSelected());
            dismiss();
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {  // При возврате в Main по кнопке Назад
        super.onCancel(dialog);
    }

    class EntityFieldArrayAdapter extends ArrayAdapter<EntityField> {
        EntityFieldArrayAdapter(Context context, ArrayList<EntityField> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View itemView, ViewGroup parent) {
            Log.d(TAG, "getView " + position);

            SpinnerContainer[] scV = {null};
            AppCompatSpinner sV;
            EditText etV;
            TextView twV;
            TextView twV2;

            if (itemView == null) {
                itemView = LayoutInflater.from(getContext()).inflate(R.layout.field_item, parent,
                        false);
                (sV = itemView.findViewById(R.id.spinnerValue)).setOnFocusChangeListener(EditDialogFragment.this);
                (etV = itemView.findViewById(R.id.editTextValue)).setOnFocusChangeListener(EditDialogFragment.this);
            } else {
                (sV = itemView.findViewById(R.id.spinnerValue)).clearFocus();
                (etV = itemView.findViewById(R.id.editTextValue)).clearFocus();
            }
            twV = itemView.findViewById(R.id.textViewValue);
            twV2 = itemView.findViewById(R.id.textViewColectionSign);

            EntityField item = getItem(position);
            itemView.setTag(position);
            paintItem(itemView, item);

            ((TextView) itemView.findViewById(R.id.textViewName)).setText(item.description);

            if (item.spinnerNeeded()) {
                sV.setVisibility(View.VISIBLE);
                etV.setVisibility(View.INVISIBLE);
                twV.setVisibility(View.INVISIBLE);
                twV2.setVisibility(View.INVISIBLE);

                sV.setEnabled(!ep.editMode.equals(EDIT_MODE_VIEW) && !item.editability.equals(EDITABILITY_NOT_EDITABLE));

                if (item.possibleValues == null) {
                    item.possibleValues = item.formReferencesArray(true);
                }
                scV[0] = new SpinnerContainer(
                        getActivity(),
                        sV,
                        item.possibleValues,  // ToDo оптимазировать - загружать только по click'y !!!
                        item.value,
                        new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                                scV[0].defaultOnItemSelectedListener.onItemSelected(adapterView, view, i, l);

                                if (scV[0].selectedItem != null) {
                                    if (item.isCollection()) {
                                        showObject(scV[0].selectedItem);
                                    } else {
                                        storeFieldValue(scV[0]);
                                    }
                                }
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> adapterView) {
                            }
                        });

            } else if (item.isCollection()) {
                sV.setVisibility(View.INVISIBLE);
                etV.setVisibility(View.INVISIBLE);
                twV.setVisibility(View.VISIBLE);
                twV2.setVisibility(View.VISIBLE);

                Log.d("qqqq", item.description);
                twV.setText(item.value == null ? "" : collectionStr(item.value));
                twV.setOnClickListener((v) -> editCollection(v, item,
                        ep.editMode.equals(EDIT_MODE_VIEW) || item.editability.equals(EDITABILITY_NOT_EDITABLE)));
                twV2.setOnClickListener((v) -> editCollection(v, item,
                        ep.editMode.equals(EDIT_MODE_VIEW) || item.editability.equals(EDITABILITY_NOT_EDITABLE)));
            } else {
                sV.setVisibility(View.INVISIBLE);
                etV.setVisibility(View.VISIBLE);
                twV.setVisibility(View.INVISIBLE);
                twV2.setVisibility(View.INVISIBLE);

                etV.setEnabled(!ep.editMode.equals(EDIT_MODE_VIEW) && !item.editability.equals(EDITABILITY_NOT_EDITABLE));

                etV.setText(item.value == null ? null : item.value.toString());
                etV.setInputType(
                        item.type.equals(TYPE_DATE) || item.type.equals(TYPE_DATETIME) ?
                                TYPE_CLASS_DATETIME | TYPE_DATETIME_VARIATION_DATE :
                                item.type.equals(TYPE_INTEGER) ? TYPE_CLASS_NUMBER :
                                        item.type.equals(TYPE_DECIMAL) ?
                                                TYPE_CLASS_NUMBER | TYPE_NUMBER_FLAG_DECIMAL :
                                                TYPE_CLASS_TEXT);
            }
            return itemView;
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) { // Вход в EditText поле
            curFocusedView = v;
        } else { // Выход из поля
            storeFieldValue(v); // В массив fieldsFields
        }
    }

    void paintItem(View itemView, EntityField item) {

        int color = getResources().getColor(
                !ep.editMode.equals(EDIT_MODE_VIEW) && item.editability.equals(EDITABILITY_MANDATORY) ?
                        R.color.colorFieldNotEmpty :
                        !ep.editMode.equals(EDIT_MODE_VIEW) && item.editability.equals(EDITABILITY_EDITABLE) ?
                                R.color.colorFieldEditable :
                                R.color.colorFieldNotEditable);
/*
        int color = getResources().getColor(
                item.editability.equals(EDITABILITY_MANDATORY) ? R.color.colorFieldNotEmpty :
                item.editability.equals(EDITABILITY_EDITABLE) ? R.color.colorFieldEditable :
                R.color.colorFieldNotEditable);
*/
        itemView.setBackgroundColor(color);
    }

    void storeFieldValue(Object o) {
        if (o == null) return;

        if (o instanceof SpinnerContainer) {
            int index = (Integer) ((View) ((SpinnerContainer) o).spinner.getParent()).getTag();
            entityFields.get(index).value = ((SpinnerContainer) o).selectedItem;
        } else if (o instanceof EditText) {
            int index = (Integer) ((View) ((View) o).getParent()).getTag();
            entityFields.get(index).value = ((TextView) o).getText().toString();
        }
    }

    static ArrayList<EntityField> updateEntityFieldsArray(
            ArrayList<EntityField> entityFieldsArray, String stepName, boolean toSort) {

        ArrayList<EntityField> r = new ArrayList<>(entityFieldsArray);
        ArrayList<String> mandatoryFields = new ArrayList<>(Arrays.asList(ESMisc.getMandatoryFields(stepName)));
        ArrayList<String> editableFields = new ArrayList<>(Arrays.asList(ESMisc.getEditableFields(stepName)));

        for (int i = r.size() - 1; i >= 0; i--) {
            EntityField ef = r.get(i);

            if (mandatoryFields.contains(ef.description)) {
                ef.editability = EDITABILITY_MANDATORY;
            } else if (editableFields.contains(ef.description)) {
                ef.editability = EDITABILITY_EDITABLE;
            } else {
                ef.editability = EDITABILITY_NOT_EDITABLE;
            }
        }
        if (toSort) {
            Collections.sort(r, (Object lhs, Object rhs) -> // По убыванию editability, по возрастанию названий
                    // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    -1000 * ((EntityField) lhs).editability.compareTo(((EntityField) rhs).editability)
                            + ((EntityField) lhs).description.compareTo(((EntityField) rhs).description));
        }
        return r;
    }

    void showList() {
        Log.d(TAG, "showList");

        getDialog().setTitle(ep.objIn.toString());

        if (mainEntityFields == null) {
            mainEntityFields = formEntityFieldsArray(mainEntityName);
            curStepName = null;
            mainEntityFields = updateEntityFieldsArray(mainEntityFields, curStepName, true);  // Порядок полей удет постоянный
            entityFields = new ArrayList<>(mainEntityFields);
        }
        if (ep.stepName != curStepName) {
            curStepName = ep.stepName;
            entityFields = updateEntityFieldsArray(mainEntityFields, curStepName, false);  // Только устанавливаем editability
        }
        if (isFragmentNewIstance) {
            int editableFieldsCount = 0;

            for (int i = entityFields.size() - 1; i >= 0; i--) {   // Установка значений из objIn
                EntityField ef = entityFields.get(i);

                if (curStepName == null && ef.editability.equals(EDITABILITY_NOT_EDITABLE)) {
                    entityFields.remove(i);  // Они пустые
                    continue;
                } else if (!ef.editability.equals(EDITABILITY_NOT_EDITABLE)) {
                    editableFieldsCount++;
                }

                ef.value = null;  // Очищаем с прошлого раза
                if (ef.reflectField != null) {
                    try {
                        Object o = ef.reflectField.get(ep.objIn);  // Значение поля
                        if (o != null) {
                            if (ef.attributeType.equals(ATTR_TYPE_ASSOCIATION)) {  // o - ссылаемый объект
                                if (!ef.isCollection()) {
                                    ef.value = new Reference((Get) o);
                                } else {
                                    ef.value = o;
                                }
                            } else if (ef.attributeType.equals(ATTR_TYPE_ENUM)) { // o - строка, name - CASH, а надо Безналичный
                                if (ef.possibleValues == null) {
                                    // TODO: 31.03.2019 Оптимизировать!
                                    ef.possibleValues = ef.formReferencesArray(true);
                                }
                                for (Object o2 : ef.possibleValues) {
                                    if (o2 != null && o.equals(((EnumItem) o2).name)) {
                                        ef.value = o2;
                                        break;
                                    }
                                }
                            } else if (ef.type.equals(TYPE_BOOLEAN)) {
                                ef.value = ((Boolean) o) ? VALUE_YES : VALUE_NO;
                            } else if (ef.type.equals(TYPE_DATE) || ef.type.equals(TYPE_DATETIME)) {
                                ef.value = SDF_OUR.format(o);
                            } else {
                                ef.value = o;
                            }
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
            if (ep.editMode.equals(EDIT_MODE_EDIT) && editableFieldsCount == 0) {
                ep.editMode = EDIT_MODE_VIEW;
            }
        }
        isFragmentNewIstance = false;

        ArrayAdapter<EntityField> adapter = new EntityFieldArrayAdapter(getActivity(), entityFields);
        fieldListView = view.findViewById(R.id.fieldItems);
        fieldListView.setAdapter(adapter);
    }

    String entity2Json() {
        Log.d(TAG, "entity2Json");

        String r = "{";
        String s;  // ToDo StringBuilder?

        for (EntityField ef : entityFields) {
            if (ef.editability.equals(EDITABILITY_NOT_EDITABLE)) continue;

            if (ef.value == null) continue;
            if (ef.value.toString() == null) continue;
            if (ef.value.toString().trim().equals("")) continue;

            s = "\"" + ef.name + "\":";
            if (ef.attributeType.equals(ATTR_TYPE_DATA) && ef.type.equals(TYPE_BOOLEAN)) {
                s = s + (ef.value.equals(VALUE_YES) ? "true," : "false,");
            } else if (ef.attributeType.equals(ATTR_TYPE_DATA) && ef.type.equals(TYPE_DATE)) {
                s = s + "\"" + Utils.dateStr2DateStr(ef.value.toString(), SDF_OUR, SDF_SERVER) + "\",";
            } else if (ef.attributeType.equals(ATTR_TYPE_DATA) && ef.type.equals(TYPE_DATETIME)) {
                // Не может быть редактируемым
            } else if (ef.attributeType.equals(ATTR_TYPE_ASSOCIATION)) {
                if (!ef.isCollection()) {
                    s = s + "{\"id\":\"" + ((Reference) ef.value).id + "\"},";
                } else {
                    s = s + "[";
                    for (Get obj : (ArrayList<Get>) ef.value) {
                        s = s + "{\"id\":\"" + obj.getId() + "\"},";
                    }
                    s = (s + "],").replace(",]", "]");
                }
            } else if (ef.attributeType.equals(ATTR_TYPE_ENUM)) {
                s = s + "\"" + ((EnumItem) ef.value).name + "\",";
            } else {
                s = s + "\"" + ef.value + "\",";
            }
            r += s;
        }
        Log.d(TAG, "entity2Json \n" + r);
        if (!r.equals("{")) return r.substring(0, r.length() - 1) + "}";
        return null;
    }

    boolean send() {
        String id = ep.objIn.getId();

        String json = entity2Json();
        if (json != null) {
            if (id == null && cr.createEntity(ep.entityName, json) ||  // Можно cr.postJSON("query/create", json)
                    id != null && cr.updateEntity(ep.entityName, id, json)) {
                try {
                    ep.objOut = OM.readValue(cr.responseBodyStr, ep.objOut.getClass());
                    Utils.message(ep.objOut +    // Должен быть toString переопределен
                            (id == null ? " создан(а)" : " изменен(а)"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            } else {
                Utils.message(ep.objIn + (id == null ? " НЕ создан(а)" : " НЕ изменен(а)") +
                        "!\n" + cr.error);
            }
        }
        return false;
    }

    void showObject(Object o) {
    }

    void editCollection(View v, EntityField entityField, boolean viewOnly) {
        EditCollectionDialogFragment.ef = entityField;
        EditCollectionDialogFragment.captionView = (TextView) v;
        EditCollectionDialogFragment.viewOnly = viewOnly;
        EditCollectionDialogFragment.isFragmentNewIstance = true;  // true - запуск фрагмента из программы, а не автоматическое пересоздание при перевороте

        DialogFragment dlg = new EditCollectionDialogFragment();
        dlg.show(getFragmentManager(), "EditCollectionDialogFragment");
    }

    static String collectionStr(Object collection) {
        return "[" + ((ArrayList<Object>) collection).size() + "]";

    }

}
