package com.gf169.smartbills;

import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Vector;

import static com.gf169.smartbills.Common.Get;

class Dataset<T extends Get> {
    static class Item<T> {
        T entity = null;
        boolean isFiltered = false;
        boolean isMarked = false;
    }

    protected Vector<Item<T>> items = new Vector<>();  // Именно вектор - доступ к нему из разных thread'ов
    private int itemSelected = -1;    // В массиве
    private int chunkSize = 0;        // Сколько item'ов грузим за 1 раз с сервера
    private int pageSize = 0;         // Сколько item'ов влезает на экран
    private volatile int[] itemCount = {0, 0};  // Число записей в массиве - прочитаных из источника и
    // Число показываемых - неудаленных, отфильтрованных = position в adapter'e
    private String sortField = null;
    private int[] lastAccessedItemIndex = {-1, -1}; // В массиве, в адаптере
    protected volatile int firstItemOfLastPage = 0;
    private int markedItemCount = 0;
    private int loadCount = 0; // Число загрузок - вызовов addPage

    String TAG = "gfDataset";

    Dataset(String tag) {
        if (tag != null) {
            TAG = tag + "/gfDataset";
        }
    }

    Item<T> getItem(int index) {
        return items.get(index);
    }

    void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    int getChunkSize() {
        return chunkSize;
    }

    void setSelected(int itemSelected) {
        this.itemSelected = itemSelected;
    }

    int getSelected() {
        return itemSelected;
    }

    String getSelectedItemId() {
        if (itemSelected >= 0) return items.get(itemSelected).entity.getId();
        return null;
    }

    void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    int getPageSize() {
        return pageSize;
    }

    void setSortField(String sortField) {
        this.sortField = sortField;
    }

    String getSortField() {
        return sortField;
    }

    int getMarkedItemCount() {
        return markedItemCount;
    }

    void setMarkedItemCount(int markedItemCount) {
        this.markedItemCount = markedItemCount;
    }

    String getMarkedItemIdsString() {
        if (markedItemCount > 0) {
            String s = " ";
            int j = 0;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).isMarked) {
                    s += items.get(i).entity.getId() + " ";
                    j++;
                    if (j == markedItemCount) return s;
                }
            }
        }
        return null;
    }

    T[] getMarkedItemEntityArray() {
        if (markedItemCount > 0) {
            T[] a = null;  // new T[markedItemCount]  Не ест!
            int j = 0;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).isMarked) {
                    if (j == 0) {
                        a = (T[]) Array.newInstance(items.get(i).entity.getClass(), markedItemCount);
                    }
                    a[j] = items.get(i).entity;
                    j++;
                    if (j == markedItemCount) return a;
                }
            }
        }
        return null;
    }

    int getFirstMarkedItemPos() {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isMarked) return i;
        }
        return -1;
    }

    int getItemCount(int i) {
        return itemCount[i];
    }

    protected ArrayList<T> loadChunk() {  // Грузим кусок с сервера
        return null;
    }

    private int[] addChunk(StringBuffer selectedItemId, StringBuffer markedItemsIds) {    // Загружаем кусок и добавляем его к dataset'у
        Log.d(TAG, "addChunk " + selectedItemId + " " + markedItemsIds);

        int[] r = {0, 0};  // Результат - сколько добавили

        ArrayList<T> chunk = loadChunk();
        Log.d(TAG, "addChunk size=" + (chunk == null ? 0 : chunk.size()));
        if (chunk == null) return r;

        r[0] = chunk.size();
        for (int i = 0; i < chunk.size(); i++) {
            Item it = new Item<T>();
            it.entity = chunk.get(i);

            String id = ((T) it.entity).getId();

            // Это выполняется при синхронном вызове, volatile не нужно
            if (itemSelected < 0 && selectedItemId != null && selectedItemId.toString().equals(id)) {
                itemSelected = itemCount[0];
                selectedItemId.delete(0, selectedItemId.length()); // ""
            }
            if (markedItemsIds != null && markedItemsIds.toString().contains(" " + id + " ")) {
                it.isMarked = true;
                markedItemCount++;
                markedItemsIds.delete(markedItemsIds.indexOf(" " + id + " "),
                        markedItemsIds.indexOf(" " + id + " ") + id.length() + 1); // 1 пробел оставляем
            }

            if (testEntity((T) it.entity)) {
                it.isFiltered = true;
                itemCount[1]++;
                r[1]++;
            }
            itemCount[0]++;
            items.add(it);
        }

        return r;
    }

    synchronized void addPage(int lastVisibleItemPosition, // Добавляем кусок, минимум 1 экран (page)
                              String selectedItemId,
                              String markedItemsIds,
                              RecyclerView itemList,
                              boolean dontLoad) { // Ничего не грузим, фильтруем уже загруженный массив
        loadCount++;
        Log.d(TAG, "addPage #" + loadCount + " " + selectedItemId + " " + markedItemsIds +
                " pageSize=" + pageSize);

        if (loadCount == 3) { // Только тогда экран нарисован и getChildCount()
            // даст правильное число items, влезающих в экран
            setPageSize(itemList.getLayoutManager().getChildCount() + 1);
            Log.d(TAG, "setPageSize(" + getPageSize() + ")");
        }

        int[] r = {0, 0};  // Результат - сколько добавили

        if (dontLoad && FilterDialogFragment.toApplyFilter) {
            firstItemOfLastPage = 1;
            itemCount[0] = items.size();
            itemCount[1] = 0;
            for (Item it : items) {
                if (testEntity((T) it.entity)) {
                    it.isFiltered = true;
                    itemCount[1]++;
                    r[1]++;
                } else {
                    it.isFiltered = false;
                }
            }
        }
        if (r[1] == 0) {
            firstItemOfLastPage = itemCount[1] + 1;  // Следующая загруженная запись будет иметь
            // эту позицию в адаптере
            int[] r2;
            StringBuffer selectedItemIdB = selectedItemId == null ? null : new StringBuffer(selectedItemId);
            StringBuffer markedItemsIdsB = markedItemsIds == null ? null : new StringBuffer(markedItemsIds);
            do {
                r2 = addChunk(selectedItemIdB, markedItemsIdsB);
                if (r2[0] == 0) {  // Дошли до конца
                    break;
                }
                r[0] += r2[0];
                r[1] += r2[1];
            }
            while (lastVisibleItemPosition >= 0 && r[1] <= lastVisibleItemPosition ||
                    selectedItemIdB != null && !selectedItemIdB.toString().isEmpty() ||
                    markedItemsIdsB != null && !markedItemsIdsB.toString().equals(" ") ||
                    r[1] < pageSize);
        }
        Log.d(TAG, "addPage added=(" + r[0] + " " + r[1] + ")");

        if (r[1] > 0 && itemList != null && itemList.getAdapter() != null) {  // Перенести в addChunk?
            itemList.post(() -> itemList.getAdapter().notifyItemRangeInserted(
                    itemList.getAdapter().getItemCount(), r[1]));
        }
    }

    int getItemIndexByPosition(int position) {  // Индекс в массиве по позиции в адаптере
        int j = lastAccessedItemIndex[1];
        if (position == j) {
            return lastAccessedItemIndex[0];

        } else if (position > j) {
            for (int i = lastAccessedItemIndex[0] + 1; i < items.size(); i++) {
                Item it = items.get(i);
                if (!it.isFiltered) {
                } else if (position > j + 1) {
                    j++;
                } else {
                    lastAccessedItemIndex[0] = i;
                    lastAccessedItemIndex[1] = position;
                    return i;
                }
            }

        } else if (position < j) {
            for (int i = lastAccessedItemIndex[0] - 1; i >= 0; i--) {
                Item it = items.get(i);
                if (!it.isFiltered) {
                } else if (position < j - 1) {
                    j--;
                } else {
                    lastAccessedItemIndex[0] = i;
                    lastAccessedItemIndex[1] = position;
                    return i;
                }
            }
        }
        return -1;
    }

    void setMark(int index, boolean value) {
        items.get(index).isMarked = value;
    }

    protected boolean testEntity(T entity) {
        if (FilterDialogFragment.toApplyFilter) {
            if (Utils.error != null) {  // Ошибка при контроле - неверная дата
                FilterDialogFragment.toApplyFilter = false;
                Utils.error = null;
                return true;
            }
            return FilterDialogFragment.testEntity(entity);
        }
        return true;
    }
}
