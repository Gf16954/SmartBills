package com.gf169.smartbills;

import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.google.android.gms.common.util.CollectionUtils;

import java.util.ArrayList;

import static android.view.Menu.NONE;
import static com.gf169.gfutils.Utils.grantMeAllDangerousPermissions;
import static com.gf169.gfutils.Utils.iniUtils;
import static com.gf169.smartbills.Common.Get;
import static com.gf169.smartbills.Common.GetWorkflow;
import static com.gf169.smartbills.Common.cr;
import static com.gf169.smartbills.Common.curActivity;
import static com.gf169.smartbills.Common.curUser;
import static com.gf169.smartbills.Common.mainEntityClass;
import static com.gf169.smartbills.Common.packageName;
import static com.gf169.smartbills.ESEntityLists.getEntityListsList;
import static com.gf169.smartbills.ESEntityLists.getFilterStr;
import static com.gf169.smartbills.ESMisc.mainEntityName;
import static com.gf169.smartbills.EntityActions.ACTION_CREATE;
import static com.gf169.smartbills.Utils.message;
import static com.gf169.smartbills.Utils.showBundleContents;
import static com.gf169.smartbills.Utils.tintMenuIcon;

public class MainActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener {
    static final String TAG = "gfMainActivity";

    public static final int LOGIN_REQUEST_CODE = 9;

    RecyclerView itemList;
    LinearLayoutManager layoutManager;
    MyAdapter adapter;
    ESDataset dataset;

    Toolbar toolbar;
    FloatingActionButton fab;
    PopupMenu actionsPopup;

    Spinner entityListSpinner;
    ArrayAdapter<CharSequence> entityListSpinnerAdapter;
    ArrayList<String> listKindNames = new ArrayList<>();
    int entityListNumber = 0;
    String filterStr;

    Bundle sis; // savedInstanceState;
    boolean dontFill; // :)
    Boolean disableInput = true; // Блокирует пользовательский ввод

    EntityActions entityActions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        curActivity = this;
        packageName = this.getPackageName();

        iniUtils(curActivity, BuildConfig.DEBUG, curActivity);
        grantMeAllDangerousPermissions();

        String s = null;
        try {
            s = packageName + ".Entities" +  // ...Entities$Query
                    mainEntityName.substring(mainEntityName.indexOf("$"));
            mainEntityClass = Class.forName(s);
        } catch (ClassNotFoundException e) {
            message("Класс " + s + " не существует!");
            return;
        }

        /*
        dontFill = true;  // Флаг для onSaveInstanceState - не заполняй, а возьми переданный и сохрани
        if (getResources().getConfiguration().orientation !=
                Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            return;
        }
*/
        dontFill = false;
        sis = savedInstanceState;

        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(""); // Убираем
        setSupportActionBar(toolbar);

        if (sis != null) {
            listKindNames = sis.getStringArrayList("listKindNames");
        }

        entityListSpinnerAdapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, listKindNames);
        entityListSpinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        entityListSpinner = findViewById(R.id.listKindSpinner);
        entityListSpinner.setAdapter(entityListSpinnerAdapter);
        entityListSpinner.setOnItemSelectedListener(this);

        actionsPopup = new PopupMenu(this, toolbar);
        actionsPopup.getMenuInflater()
                .inflate(R.menu.menu_actions, actionsPopup.getMenu());  // Пока в нем только заголовок
        actionsPopup.setOnMenuItemClickListener(item -> {
            entityActions.execAction(item.getTitle().toString());
            return true;
        });
/*
        actionsPopup.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu popupMenu) {

            }
        });
*/

        fab = findViewById(R.id.fab);
        fab.setOnClickListener((View view) -> showActionsPopup(-1));
        fab.hide();

        entityListNumber = 0; // Todo - из настройки
        if (sis != null) {
            entityListNumber = sis.getInt("entityListNumber", entityListNumber);
        }

        if (sis != null &&
                (cr = new CubaRequester(sis)) != null && cr.isReady()) {
            showList(false, null, null);   // Наконец!
        } else {
            login();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(savedInstanceState);

        if (dontFill) {
            if (sis != null) savedInstanceState.putAll(sis);
            return;
        }

        savedInstanceState.putInt("entityListNumber", entityListNumber);
        if (cr != null && cr.isReady()) {
            cr.saveState(savedInstanceState);
            if (dataset != null) {
                savedInstanceState.putInt("itemCount", dataset.getItemCount(0)); // Число записей в массиве
                savedInstanceState.putString("itemSelected", dataset.getSelectedItemId());
                savedInstanceState.putInt("chunkSize", dataset.getChunkSize());
//                savedInstanceState.putInt("pageSize", dataset.getPageSize());
                savedInstanceState.putString("sortField", dataset.getSortField());
                savedInstanceState.putString("markedItems", dataset.getMarkedItemIdsString());
//                savedInstanceState.putParcelableArrayList("d",dataset.items);  // Todo ?
            }
            if (layoutManager != null) {
                savedInstanceState.putParcelable("layoutManagerState",  // На самом деле первая строка на экране
                        layoutManager.onSaveInstanceState());
                savedInstanceState.putInt("lastVisibleItemPosition", layoutManager.findLastVisibleItemPosition());
            }
            if (listKindNames != null) {
                savedInstanceState.putStringArrayList("listKindNames", listKindNames);
            }
            showBundleContents(TAG, savedInstanceState);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (disableInput) return true;
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (curUser != null) {
            entityActions = new EntityActions(null, entityListNumber);
            menu.findItem(R.id.action_new).setVisible(entityActions.actionIsPossible(ACTION_CREATE));
        }
        paintSearchIcon(menu.findItem(R.id.action_search));

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new:
                editEntity(null, false);
                return true;

            case R.id.action_search:
                DialogFragment dlg = new FilterDialogFragment();
                dlg.show(getFragmentManager(), "FilterDialogFragment");
//                AppCompatDialogFragment dlg = new FilterDialogFragment();
//                dlg.show(getSupportFragmentManager(), "dlg");
                return true;

            case R.id.action_refresh:
                showList(false,
                        dataset == null ? null : dataset.getSelectedItemId(),
//                    dataset == null ? null : dataset.getMarkedItemIdsString());
                        null);
                return true;

            case R.id.action_settings:
                Utils.message("Еще не сделано");
                return true;

            case R.id.action_help:
                Utils.message("Еще не сделано");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        curActivity = null;     // Против утечки
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view,  // Для entityListSpinner'a
                               int pos, long id) {
        if (pos != entityListNumber) {
            entityListNumber = pos;
            FilterDialogFragment.toApplyFilter = false; // ToDo ?
            showList(false, null, null);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public void setFABvisibility() {
        if (dataset.getMarkedItemCount() > 0) {
            fab.show();
        } else fab.hide();
    }

    public void showActionsPopup(int itemIndex) {
        Menu menu = actionsPopup.getMenu();

        GetWorkflow[] entities;

        if (itemIndex >= 0) { // Действия с одной заявкой
            GetWorkflow[] entities2 = {dataset.getItem(itemIndex).entity};
            entities = entities2;

            menu.getItem(0).setTitle(
                    entities[0] + ":");

        } else {         // Действия с группой отмеченных заявок
            entities = dataset.getMarkedItemEntityArray();

            if (dataset.getMarkedItemCount() == 1) {
                menu.getItem(0).setTitle(entities[0] + ":");
            } else {
                String s;
                int i = dataset.getMarkedItemCount();
                int j = i % 10;
                s = " отмеченных:";
                if (j == 1) s = " отмеченную:";
                else if (j > 1 && j < 5) s = " отмеченные:";
                s = i + s;
                menu.getItem(0).setTitle(s);
            }
        }

        entityActions = new EntityActions(entities, entityListNumber);

        while (menu.size() > 1) { // Чистим, оставляем заголовок
            menu.removeItem(1);
        }
        ArrayList<String> menuItems = entityActions.getAvailableActions();
        while (menu.size() > 1) { // Чистим, оставляем заголовок
            menu.removeItem(1);
        }
        for (int i = 0; i < menuItems.size(); i++) {
            menu.add(NONE, 1, NONE, menuItems.get(i));  // Просто add добавляет с Id=0 - неотличим от заголовка
        }
        if (menu.size() == 1) {
            message("Нет доступных действий");
        } else if (menu.size() == 2 &&  // Есть только Просмотреть - сразу проваливаемся
                entityActions.getActionByDisplayName(menu.getItem(1).getTitle().toString()).
                        equals(EntityActions.ACTION_EDIT)) {
            entityActions.execAction(EntityActions.ACTION_EDIT);
        } else {
            actionsPopup.show();
        }
    }

    public void login() {
        Log.d(TAG, "login");

        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, LOGIN_REQUEST_CODE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == LOGIN_REQUEST_CODE) {
            if (cr == null || !cr.isReady()) {  // В экране логина нажажи Назад
                finish();
                return;
            }
            if (new Experiment().exec()) return;

            if ((curUser = Entities.ExtUser.build(null)) == null) {
                Utils.message("Не удалось получить параметры текущего пользователя");
                cr = null;
            }
            if (cr != null) {
                dontFill = false;  // Уже токен получен, его надо будет сохранить
                if (!CollectionUtils.isEmpty(listKindNames))
                    listKindNames.clear();  // Пользователь сменился (возможно)


                showList(false, null, null);

            }
        }
    }

    void showList(boolean dontReload, String selectedItemId, String markedItemsIds) {
        Log.d(TAG, "showList " + dontReload);

        invalidateOptionsMenu();

        disableInput = true;  // Пока не покажем

        if (listKindNames.size() == 0) { // Формируем массив видов списка и вставляем в spinner
            listKindNames.addAll(getEntityListsList());
            entityListSpinnerAdapter.notifyDataSetChanged();
        }

        filterStr = getFilterStr(listKindNames.get(entityListNumber));
        // if (filterStr == null) return;

        dataset = new ESDataset("tag");
        int chunkSize = 50;   // ToDo
        dataset.setChunkSize(sis != null ? sis.getInt("chunkSize", chunkSize) : chunkSize);
        dataset.setPageSize(sis != null ?          // Начальное значение, возможно, будет изменено
                sis.getInt("pageSize", dataset.getChunkSize()) : dataset.getChunkSize());
        dataset.setSortField(sis != null ?           // По убыванию даты создания   // Не используется
                sis.getString("sortField", "-createTs") : "-createTs");

        int lastVisibleItemPosition = (sis != null ? sis.getInt("lastVisibleItemPosition", 0) : -1);
        selectedItemId = (sis != null ? sis.getString("itemSelected", selectedItemId) :
                selectedItemId);
        markedItemsIds = (sis != null ? sis.getString("markedItems", markedItemsIds) :
                markedItemsIds);
        // Синхронно грузим первую страницу ToDo сохранять?
        dataset.addPage(lastVisibleItemPosition, selectedItemId, markedItemsIds, itemList, dontReload);

        adapter = new MyAdapter(dataset, R.layout.entity_item);

        itemList = findViewById(R.id.items);
        itemList.setHasFixedSize(true);
        itemList.setAdapter(adapter);

        layoutManager = new LinearLayoutManager(this) {
/*
            @Override
            public void onLayoutCompleted (RecyclerView.State state) {
                super.onLayoutCompleted(state);
                int iF = layoutManager.findFirstVisibleItemPosition();
                int iL = layoutManager.findLastVisibleItemPosition();
                Log.d(TAG, "onLayoutCompleted: "+iF+" "+iL);
                int pos = dataset.getSelected();
                if (pos >= 0) {
                    if (pos < iF || pos > iL) {  // Если выделенный не виден, его в первую строку
                        layoutManager.scrollToPositionWithOffset(pos, 0);
                        // itemList.smoothScrollToPosition(pos); Или так
                    }
                } else {    // Выделенного нет - первый из отмеченных
                    pos = dataset.getFirstMarkedItemPos();
                    if (pos >= 0) {
                        if (pos < iF || pos > iL) {
                            layoutManager.scrollToPositionWithOffset(pos, 0);
                        }
                    }
                }
            }
*/
        };
        itemList.setLayoutManager(layoutManager);
        if (sis != null) {  // Восстанавливаем позицию после переворота (и так восстановится)
            // и после восстановления после убийства (!!!)
            Parcelable layoutManagerState = sis.getParcelable("layoutManagerState");
            if (layoutManagerState != null) {
                layoutManager.onRestoreInstanceState(layoutManagerState);
            }
        }
        itemList.post(() -> {  // Возвращаем выделенный или первй отмеченный на экран - post !
            int iF = layoutManager.findFirstVisibleItemPosition();
            int iL = layoutManager.findLastVisibleItemPosition();
            int pos = dataset.getSelected();
            if (pos >= 0) {
                if (pos < iF || pos > iL) {  // Если выделенный не виден, его в первую строку
                    layoutManager.scrollToPositionWithOffset(pos, 0);
                    // itemList.smoothScrollToPosition(pos); Или так
                }
            } else {    // Выделенного нет - первый из отмеченных
                pos = dataset.getFirstMarkedItemPos();
                if (pos >= 0) {
                    if (pos < iF || pos > iL) {
                        layoutManager.scrollToPositionWithOffset(pos, 0);
                    }
                }
            }
        });

        setFABvisibility();

        sis = null;  // Больше не нужен
        disableInput = false;
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        protected View v;  // Layout
        protected CheckBox vIsMarked;

        protected MyViewHolder(View v) {
            super(v);
            this.v = v;

            vIsMarked = v.findViewById(R.id.checkBoxMark);
            vIsMarked.setOnClickListener((View v2) -> markItem(vIsMarked, false));

            v.setOnClickListener(this::selectItem);
            v.setOnLongClickListener((View v2) -> {
                markItem(vIsMarked, true);
                showActionsPopup(-1);
                return true;
            });
        }

        private void markItem(CheckBox v2, boolean fromLongClick) {
            int delta = 0;
            if (fromLongClick) {
                if (!v2.isChecked()) {
                    v2.setChecked(true);  // Сами ставим птичку
                    delta = 1;
                }
            } else {   // Птичка уже поставлена/снята
                delta = v2.isChecked() ? 1 : -1;
            }

            int index = (Integer) (((View) v2.getParent()).getTag());
            dataset.setMark(index, v2.isChecked());
            dataset.setMarkedItemCount(dataset.getMarkedItemCount() + delta);

            setFABvisibility();
        }

        private void selectItem(View v2) {
            Log.d(TAG, "selectItem");

            int index = (Integer) v2.getTag();
            int indexSelected = dataset.getSelected();
            if (index == indexSelected) {
                showActionsPopup(index);
            } else {
                dataset.setSelected(index);
                paintItem(v2, true, dataset.getItem(index).entity);
                View v3 = ((View) v2.getParent()).findViewWithTag(indexSelected);
                if (v3 != null) { // На экране или рядом
                    paintItem(v3, false, dataset.getItem(indexSelected).entity);
                }
                showActionsPopup(index);
            }
        }

        protected void fillFields(Object entity, boolean isMarked, boolean isSelected) {

            vIsMarked.setChecked(isMarked);

            paintItem(v, isSelected, entity);
        }

        protected void paintItem(View v, boolean isSelected, Object o) {
            int color = getResources().getColor(isSelected ?
                    R.color.colorItemSelected :
                    R.color.colorItem
            );
            v.setBackgroundColor(color);
        }
    }

    class MyAdapter extends RecyclerView.Adapter<ESMyViewHolder> {
        private Dataset dataset;
        private int layoutResource;

        MyAdapter(Dataset dataset, int layoutResource) {
            this.dataset = dataset;
            this.layoutResource = layoutResource;
        }

        @Override
        public ESMyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Log.d(TAG, "onCreateViewHolder");

            View v = LayoutInflater.from(parent.getContext())
                    .inflate(layoutResource, parent, false);
            return new ESMyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ESMyViewHolder holder, int position) {
            int index = dataset.getItemIndexByPosition(position);
            Log.d(TAG, "onBindViewHolder " + position + " " + index + " " + dataset.firstItemOfLastPage);

            ESDataset.Item it = (ESDataset.Item) dataset.items.get(index);
            if (it != null) {
                holder.fillFields(it.entity,
                        it.isMarked, index == dataset.getSelected());
                holder.v.setTag(index);  // Прицепляем
            }
            if (dataset.getPageSize() > 0 && position == dataset.firstItemOfLastPage) {  // ToDo ???
                // Первый item последнего загруженного chunk'a появился
                // на экране - пора грузить новую
                new Thread(() -> dataset.addPage(0, null, null,  // Асинхронно
                        itemList, false)).start();
            }
        }

        @Override
        public int getItemCount() {
            return dataset.getItemCount(1);
        }
    }

    void editEntity(Object entity, boolean viewMode) {
        String editMode =
                entity == null ? EditDialogFragment.EDIT_MODE_CREATION :
                        !viewMode ? EditDialogFragment.EDIT_MODE_EDIT :
                                EditDialogFragment.EDIT_MODE_VIEW;
        Get eIn = null;
        if (entity == null) { // Создание
            try {
                eIn = (Get) mainEntityClass.newInstance();
                ESMisc.iniEntity(eIn);
            } catch (Exception e) {
                message(e.toString());
                e.printStackTrace();
                return;
            }
        } else {
            eIn = (Get) entity;
        }

        Get eOut = null;
        try {
            eOut = (Get) mainEntityClass.newInstance();
        } catch (Exception e) {
            message(e.toString());
            e.printStackTrace();
            return;
        }
        EditDialogFragment.ep = new EditDialogFragment.EditParameters(
                editMode,
                mainEntityName,
                eIn, eOut);

        if (!editMode.equals(EditDialogFragment.EDIT_MODE_VIEW)) { // Будет вызван showList
            (sis = new Bundle()).putParcelable("layoutManagerState",
                    layoutManager.onSaveInstanceState());
        }
        EditDialogFragment.isFragmentNewIstance = true;  // true - запуск фрагмента из программы, а не автоматическое пересоздание при перевороте
        EditDialogFragment dlg = new EditDialogFragment();
        dlg.show(getFragmentManager(), "EditDialogFragment");
    }

    void processEntity(Object[] entities) {
        String mes = "", sOk = "OK", sNotOk = "Ошибка: ";

        for (Object entity : entities) {
            mes += entity + " - " + ESMisc.processEntity(entity, sOk, sNotOk) + "\n";
        }
        message(mes);

        if (mes.contains(sOk + "\n")) { // Хоть одна усешно
            (sis = new Bundle()).putParcelable("layoutManagerState",
                    layoutManager.onSaveInstanceState());
            showList(false,  // Показываем список
                    dataset.getSelectedItemId(),  // Хотим сохранить выделение
//                    dataset.getMarkedItemIdsString()); // и отметки
                    null);
        }
    }

    void deleteEntity(Object[] entities) {  // TODO: 22.03.2019 Добавить предупреждение
        String sOk = "удален(а)", sNotOk = "ошибка: ", mes = "", s;

        for (Object entity : entities) {
            if (cr.deleteEntitity(mainEntityName, ((Get) entity).getId())) {
                s = sOk;
            } else {
                s = sNotOk + cr.error;
            }
            mes += entity + " - " + s + "\n";
        }
        message(mes);

        if (mes.contains(sOk + "\n")) { // Хоть одна усешно
            (sis = new Bundle()).putParcelable("layoutManagerState",
                    layoutManager.onSaveInstanceState());
            showList(false,  // Показываем список
                    dataset.getSelectedItemId(),  // Хотим сохранить выделение
//                    dataset.getMarkedItemIdsString()); // и отметки
                    null);
        }
    }

    void copyEntity(Object entity) {
        try {
            Object o = Common.OM.readValue(Common.OM.writeValueAsString(entity), mainEntityClass);
            ESMisc.clearEntity(o);
            editEntity(o, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void paintSearchIcon(MenuItem menuItem) {
        int color = FilterDialogFragment.toApplyFilter ?
                android.R.color.holo_blue_bright :
                android.R.color.darker_gray;
        tintMenuIcon(this, menuItem, color);
    }

}

// Todo
// +1. OnSaveInstance и т.д.
// 2. Сортировка?
// 3. Номер текущей записи показать?
// -4. Фильтр по срочности?
// 5. adapter.notifyDataSetChanged() после загрузки каждого chunk'a, а не всей страницы ?
// 6. Сохранять items в parcel?
// 7. Progress spinner везде, где нужно (при загрузке первого chunk'a)
// +8. Стоя не влезает
// +9. Class utils
// 10. Показывать число загруженных записей ?
// +11. В первых 2 списках показывать статус заявки
// 12. FAB перерисовать с числом выбранных
// +13. Дабавить refresh
// 14. Вложения
// 15. Оптимизировать delete (добавить поле)?
// 16. После создания сразу в работу
// 17. Предупреждение перед удалением
// 18. Переделать фильтр - неудобный

/*  TODO: 02.04.2019
+- 1. Прикрепление вложений 11.4 Сделано частично: только локальные, только картинки, только активный выбор
2. Проталкивание заявки дальше из состояния Финансовый контроль
3. Наведение красоты:
    3.1. Подбор иконкок, на рабочий стол и в action bar
    3.2. Подбор цветов
    3.3. Точный учет размеров экрана, в т.ч. планшет/ландшафт (показ
    текущей заявки рядом со списком, как в gmail)
    3.4. Индикатор ожидания (песочные часы) везде где нужно: при логине,
    при загрузке первой порции заявок, ...
    3.5. На FAB (красной кнопке) писать число отмеченных заявок - трудно:(
    3.6. Анимацию куда-нибудь
4. Оптимизация везде
    4.1. При редактировании/просмотре при скролинге списка полей спотыкается
    ...
5. Переделать интерфейс фильтра - неудобный, можно редактировать только последнюю запись
6. Сортировка? Непонятно как делать
7. Автоматическое обновление access token'a
8. Решение проблемы несостоятельности страничной загрузки
9. Перелогин
10. Настойка? Непонятно, что туда пихать
11. Хелп? Нужен? Или и так все ясно?
12. Слова в ресурсы? Может ли понадобиться не русский интерфейс?
13. Регистрация нового пользователя?
*/