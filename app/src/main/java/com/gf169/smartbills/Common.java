package com.gf169.smartbills;

import android.app.Activity;
import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.AppCompatSpinner;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.util.CollectionUtils;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import static com.gf169.smartbills.Utils.message;

public class Common {
    static final String TAG = "gfCommon";

    static final SimpleDateFormat SDF_SERVER = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    static final SimpleDateFormat SDF_OUR = new SimpleDateFormat("dd.MM.yyyy");

    static final ObjectMapper OM = new ObjectMapper()
            .setDateFormat(SDF_SERVER)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    static final String ROLE_USER = "Пользователь";

    static String packageName;
    static Activity curActivity; // Не уберу!
    static MainActivity mainActivity;
    static CubaRequester cr;
    static Class mainEntityClass;
    static Entities.ExtUser curUser;
    static Entities.Employee curEmployee;
    static Workflow2.Workflow curWorkflow;

    public interface Get {
        String getId();

        String getInstanceName();
    }

    public interface GetWorkflow {
        String getStepName();
        String getStatus();

        String getWorkflowId();
        boolean hasActor(Entities.ExtUser extUser);
    }

    public static class EnumItem implements Get {

        @JsonProperty("name")
        public String name;
        @JsonProperty("id")
        public String id;
        @JsonProperty("caption")
        public String caption;

        public EnumItem(@JsonProperty("name") String name,
                        @JsonProperty("id") String id,
                        @JsonProperty("caption") String caption) {
            this.name = name;
            this.id = id;
            this.caption = caption;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getInstanceName() {
            return caption;
        }

        @Override
        public String toString() {
            return caption;
        }

        @Override
        public boolean equals(Object o) {
            return o != null && (
                    ((EnumItem) o).caption == null && caption == null ||
                            ((EnumItem) o).caption != null && ((EnumItem) o).caption.equals(caption));
        }
    }

    static final String ATTR_TYPE_ASSOCIATION = "ASSOCIATION";
    static final String ATTR_TYPE_DATA = "DATATYPE";
    static final String ATTR_TYPE_ENUM = "ENUM";

    static final String TYPE_INTEGER = "int";
    static final String TYPE_DECIMAL = "decimal";
    static final String TYPE_BOOLEAN = "boolean";
    static final String TYPE_STRING = "string";
    static final String TYPE_DATE = "date";
    static final String TYPE_DATETIME = "dateTime";
    static final String TYPE_UUID = "uuid";

    static final String VALUE_YES = "Да";
    static final String VALUE_NO = "Нет";

    static final String EDITABILITY_NOT_EDITABLE = "0";
    static final String EDITABILITY_EDITABLE = "00";
    static final String EDITABILITY_MANDATORY = "000";

    public static class EntityField {
        public String name;
        public String description;
        public String attributeType;
        public String type;
        public String cardinality;

        ArrayList<Object> possibleValues;
        Object value;

        Field reflectField;
        String editability;

        /* !!!
        Если есть обычный конструктор, Object Mapper отваливается!!! "no Creators, like default construct, exist"
        Надо или чтобы его вообще не было, или чтобы параметры были с аннотациями
        Здесь он нужен, потому что в 7-ке (!!!) ObjectMapper не может без конструктора создать объект, потому что, дескать,
        java.lang.reflect.Field has no default constructor (таки нет).
        */
        public EntityField(@JsonProperty("name") String name,
                           @JsonProperty("description") String description,
                           @JsonProperty("attributeType") String attributeType,
                           @JsonProperty("type") String type) {
            this.name = name;
            this.description = description;
            this.attributeType = attributeType;
            this.type = type;
        }

        @Override
        public String toString() {
            return description; // Будет показываться в spinner'e
        }

        boolean spinnerNeeded() {
            return ATTR_TYPE_ASSOCIATION.equals(attributeType) && !isCollection() ||
                    ATTR_TYPE_DATA.equals(attributeType) && TYPE_BOOLEAN.equals(type) ||
                    ATTR_TYPE_ENUM.equals(attributeType);
        }

        Class getReferencedClass() {
            try {  // bills$Employee -> com.gf16954.smartbills.Entities$Employee
                String s = type.substring(type.indexOf("$"));
//                if (s.endsWith("$ExternalFileDescriptor")) s="$Image";
                s = packageName + ".Entities" + s;
                return Class.forName(s);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        ArrayList<Object> formReferencesArray(boolean toAddEmpty) {
            // Массив значений ссылочных полей - String, Reference или EnumItem
            ArrayList<Object> a = new ArrayList();

            if (TYPE_BOOLEAN.equals(type)) {
                a = new ArrayList<>(Arrays.asList("Да", "Нет"));
                if (toAddEmpty) {
                    a.add(0, "");  // Пустой
                }
            } else if (ATTR_TYPE_ASSOCIATION.equals(attributeType)) {
                ArrayList<Get> set = searchEntities(
                        type, // bills$Company
                        getReferencedClass(), // Entity$Company.class
                        null, null, null, null, null);
                if (!CollectionUtils.isEmpty(set)) {
                    for (Get o : set) {
                        a.add(new Reference(o));
                    }
                    Collections.sort(a, (Object lhs, Object rhs) -> // По возрастанию названий
                            lhs.toString().compareTo(rhs.toString()));
                    if (toAddEmpty) {
                        a.add(0, new Reference(null));  // Пустой
                    }
                }
            } else if (ATTR_TYPE_ENUM.equals(attributeType)) {
                Set<EnumItem> set = getEnum(type, packageName);
                if (!CollectionUtils.isEmpty(set)) {
                    a = new ArrayList<>(set);  // Массив объектов, toString вернет caption
                    Collections.sort(a, (Object lhs, Object rhs) -> // По возрастанию названий
                            lhs.toString().compareTo(rhs.toString()));
                    if (toAddEmpty) {
                        a.add(0, new EnumItem(null, null, null));  // Пустой
                    }
                }
            }
            if (!CollectionUtils.isEmpty(a)) {
/* Android 7 (а может и меньше, но не меньше 4) не вносит null в качестве элемента spinner'a
   Отваливается: Attempt to invoke virtual method 'java.lang.String java.lang.Object.toString()' on a null object reference
            if (toAddEmpty) {
                a.add(0,null);
            }
*/
            } else {
                Utils.message("Ошибка при получении списка значений поля " +
                        description + "\n" + cr.error);
            }
            return a;
        }

        boolean isCollection() {
            return cardinality.endsWith("_TO_MANY");
        }

        boolean isAttachments() {
            return type.endsWith("$ExternalFileDescriptor");
        }
    }

    static ArrayList<EntityField> formEntityFieldsArray(String entityName) {
        ArrayList<EntityField> r;

        HashMap<String, Field> classFieldsMap = new HashMap<>();
        for (Field f : mainEntityClass.getDeclaredFields()) {
            classFieldsMap.put(f.getName(), f);
        }

        if (cr.getSomething("metadata/entities/" + entityName)) {
            try {
                String s = cr.responseBodyStr;
                s = s.substring(s.indexOf("["));
                s = s.substring(0, s.length() - 1);
                r = OM.readValue(s, new TypeReference<ArrayList<EntityField>>() {
                });

                for (int i = r.size() - 1; i >= 0; i--) {
                    EntityField ef = r.get(i);
                    Field f = classFieldsMap.get(ef.name);
                    if (f == null) {  // Будем показывать только те, что есть в используемом view (из которого получен класс)
                        r.remove(i);
                        continue;
                    }
                    ef.reflectField = f;
                    ef.reflectField.setAccessible(true);
                }
                Collections.sort(r, (Object lhs, Object rhs) -> // В алфафитном порядке русских названий
                        ((EntityField) lhs).description.compareTo(((EntityField) rhs).description)
                );

                return r;

            } catch (Exception e) {
                e.printStackTrace();
                Utils.message("Ошибка при получении списка полей сущности " +
                        entityName + "\n" + e.getMessage());
            }
        } else {
            Utils.message("Ошибка при получении списка полей сущности " +
                    entityName + "\n" + cr.error);
        }
        return new ArrayList<>();
    }

    public static class SpinnerContainer {
        AppCompatSpinner spinner;
        ArrayList<Object> items;
        ArrayAdapter<Object> adapter;
        Object selectedItem;

        AdapterView.OnItemSelectedListener defaultOnItemSelectedListener =
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        Log.d(TAG, "onItemSelected default " + SpinnerContainer.this.items.get(i)); // = SpinnerContainer.this.spinner.getSelectedItem());
                        SpinnerContainer.this.selectedItem = SpinnerContainer.this.items.get(i);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                    }
                };

        SpinnerContainer(Context context,
                         AppCompatSpinner spinner,
                         ArrayList<Object> items,
                         Object selectedItem,
                         AdapterView.OnItemSelectedListener onItemSelectedListener) {
            Log.d(TAG, "SpinnerContainer " + spinner + " " + selectedItem);

            this.spinner = spinner;

            this.items = items == null ? new ArrayList<>() : items;

            adapter = new ArrayAdapter(context,
                    android.R.layout.simple_spinner_item,
                    this.items);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            this.spinner.setAdapter(adapter);

            this.selectedItem = selectedItem;
            if (selectedItem != null) {
                spinner.setSelection(this.items.indexOf(selectedItem));
            }

            if (onItemSelectedListener == null) {
                this.spinner.setOnItemSelectedListener(defaultOnItemSelectedListener);
            } else {
                this.spinner.setOnItemSelectedListener(onItemSelectedListener);  // Должен вызвать default'ный
            }
        }
    }

    static <T> ArrayList<T> searchEntities(
            String entityName,
            Class cl,
            String view,
            String filterProperty,
            String filterOperator,
            String filterValue,
            String sort) {

        Log.d(TAG, "searchEntities " +
                entityName + " " + filterProperty + " " + filterOperator + " " + filterValue);

        ArrayList<T> result = null;

        String filterStr = null;
        if (filterProperty != null) {
            filterStr = "{\"conditions\":[{" +
                    "\"property\":\"" + filterProperty + "\"," +
                    "\"operator\":\"" + filterOperator + "\"," +
                    "\"value\":";
            filterStr += filterOperator.equals("in") ? "[" + filterValue + "]" : "\"" + filterValue + "\"";
            filterStr += "}]}";
        }

        if (cr.getEntitiesList(entityName
                , filterStr, view, 0, 0, sort
                , false, false, false)) {
            try {
                result = OM.readValue(cr.responseBodyStr,
                        OM.getTypeFactory().constructCollectionType(ArrayList.class, cl));
            } catch (Exception e) {
                message(e.toString());
                e.printStackTrace();
            }
        } else {
            message("Ошибка при поиске сущностей - " + entityName);
        }
        Log.d(TAG, "searchEntities " + (result == null ? "null" : result.toString()));
        return result;
    }

    static Set<EnumItem> getEnum(String enumName, String packageName) {
        Log.d(TAG, "getEnum " + enumName);

        Set<EnumItem> result = null;
        if (cr.getSomething("metadata/enums/" + enumName)) {
            try {
                String s = cr.responseBodyStr;
                s = s.substring(s.indexOf("["), s.length() - 1);  // [...]
                result = OM.readValue(s,
                        OM.getTypeFactory().constructCollectionType(Set.class,
                                Class.forName(packageName + ".Common$EnumItem")));
            } catch (Exception e) {
                e.printStackTrace();
                message(e.getMessage());
            }
        } else {
            message("Ошибка при поиске сущностей");
        }

        Log.d(TAG, "getEnum " + (result == null ? "null" : result.toString()));
        return result;
    }

    public static class Reference {
        @JsonProperty("_instanceName")
        String _instanceName;
        @JsonProperty("id")
        String id;

        public Reference(Get o) {
            if (o != null) {
                this._instanceName = o.getInstanceName(); // !!!
                this.id = o.getId();
            }
        }

        @Override
        public String toString() {
            return _instanceName;  // Будет показываться в spinner'e
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (id == null) return ((Reference) o).id == null;
            return id.equals(((Reference) o).id);
        }
    }

    public static ArrayList<String> getEditableFields(String workflowId, String stepName) {
        if (stepName == null) {
            return new ArrayList<>(Arrays.asList(ESMisc.getEditableFields()));
        } else {
            return Workflow2.getEditableFields(workflowId, stepName);
        }
    }

    static <T extends Get> String ids2Str(Collection<T> col) {  // Для строки фильтра
        if (col == null) return null;
        String s = "";
        for (T o : col) s += o.getId() + ",";
        return s.equals("") ? "" : s.substring(0, s.length() - 1);
    }

    static void adjustFragmentSize(View rootView, float relativeHeight, float relativeWidth) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        curActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        if (height<width) { // landscape
            int i = height;
            height = width;
            width = i;
        }

        height = (int) (height * relativeHeight);
        width = (int) (width * relativeWidth);
        ConstraintLayout.LayoutParams lp =
                new ConstraintLayout.LayoutParams(width, height);
        //rootView.setLayoutParams(lp);
        ((ViewGroup) rootView).getChildAt(0).setLayoutParams(lp);
    }
}
