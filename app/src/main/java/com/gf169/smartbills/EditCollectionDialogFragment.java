package com.gf169.smartbills;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import static com.gf169.smartbills.Common.EntityField;
import static com.gf169.smartbills.Common.cr;
import static com.gf169.smartbills.Common.curActivity;
import static com.gf169.smartbills.EditDialogFragment.collectionStr;
import static com.gf169.smartbills.Utils.message;
import static com.gf169.smartbills.Utils.pickFile;
import static com.gf169.smartbills.Utils.viewFile;

//public class FilterDialogFragment extends AppCompatDialogFragment implements View.OnClickListener {
public class EditCollectionDialogFragment extends DialogFragment
        implements View.OnClickListener {
    static final String TAG = "gfCollectionDialogFragm";

    static boolean isFragmentNewIstance;    // true - запуск фрагмента из программы,
    // а не автоматическое пересоздание при перевороте
    // ToDo сохранение/восстановление

    static EntityField ef;
    static TextView captionView;
    static boolean viewOnly;

    View view;  // корневой этого фрагмента
    ListView listView;
    static ArrayList<Object> collectionItems;  // А также сохраняется при перевороте! Можно не делать onSaveInstanceState !

    private static final int PICK_FILE_REQUEST_CODE = 1;

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
        getDialog().setTitle(ef.description);

        view = inflater.inflate(R.layout.fragment_edit_collection, null);
//        adjustFragmentSize(view,0.5F, 0.9F);
// TODO: 31.03.2019 Заставить работать правильно         

        if (viewOnly) {
            view.findViewById(R.id.buttonCollectionOk).setVisibility(View.GONE);
            view.findViewById(R.id.buttonCollectionCancel).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.buttonCollectionOk).setOnClickListener(this);
            view.findViewById(R.id.buttonCollectionCancel).setOnClickListener(this);
        }

        showList();

        return view;
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onItemSelected: qqqqq");
        if (v == v.findViewById(R.id.buttonCollectionOk)) {
            // Ничего
        } else if (v == v.findViewById(R.id.buttonCollectionCancel)) {
            // Ничего
        }

        // Возвращаемся
        if (collectionItems.size() == 1) {
            ef.value = null;
            captionView.setText("");
        } else {
            collectionItems.remove(collectionItems.size() - 1);  // Последний пустой
            ef.value = new ArrayList<>(collectionItems);
            captionView.setText(collectionStr(collectionItems));
        }

        dismiss();
    }

    class collectionItemsArrayAdapter extends ArrayAdapter<Object> {
        collectionItemsArrayAdapter(Context context, ArrayList<Object> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View itemView, ViewGroup parent) {
            Log.d(TAG, "getView " + position);

            if (itemView == null) {
                itemView = LayoutInflater.from(getContext()).inflate(R.layout.collection_item, parent,
                        false);
            }
            TextView twV[] = {null};

            Object collectionItem = getItem(position);

            twV[0] = itemView.findViewById(R.id.textViewSelectedValue);
            twV[0].setText(collectionItem.toString());
            twV[0].setOnClickListener((View view) -> viewItemContents((Entities.ExternalFileDescriptor) collectionItem));

            Button button = itemView.findViewById(R.id.buttonCollectionItemAddRemove);
            if (viewOnly) {
                button.setVisibility(View.GONE);
            } else {
                if (position == collectionItems.size() - 1) {
                    button.setText("+");
                } else {
                    button.setText("X");
                }
                button.setOnClickListener((view) -> {
                    if (((Button) view).getText().equals("+")) {  // Сохраняем в массив
                        addItem();
                    } else { // "-"
                        collectionItems.remove(position);
                        ((ArrayAdapter<Object>) listView.getAdapter()).notifyDataSetChanged();
                    }
                });
            }

            return itemView;
        }
    }

    void addItem() {
        if (ef.isAttachments()) {
            String[] mimeTypes =
                    {"image/*", "application/pdf", "application/msword", "application/vnd.ms-excel"};
            pickFile(mimeTypes, this, PICK_FILE_REQUEST_CODE);
        } else {
            message("Еще не сделано");
        }
    }

    void viewItemContents(Entities.ExternalFileDescriptor efd) {
        DoInBackground.run(curActivity, () -> {
            if (ef.isAttachments()) {
                String dir = curActivity.getFilesDir().getAbsolutePath();  // Соответствует элементу files-path в provider_paths.xml
                String fullPath = cr.downloadFile(efd.id, dir, null); //efd.name); Будет грузить под одним и тем же именем - downloaded....
                if (fullPath != null) {
                    viewFile(cr.responseBodyStr);
                } else {
                    message("Ошибка при загрузке файла c сервера\n" + cr.error);
                }
            } else {
                message("Еще не сделано");
            }
        });
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
/*
        view.getViewTreeObserver().addOnPreDrawListener(myListener);
        ((ViewGroup) getActivity().findViewById(android.R.id.content)).getChildAt(0).
            post(()->{
                for (int i = 0; i < 100000; i++) {
                    for (int j = 0; j < 10000; j++) {}
                }
            });
*/
/*
        if (resultDataX!=null) {
//            onActivityResult2(requestCodeX, resultCodeX, resultDataX);
            ((ViewGroup) getActivity()
                    .findViewById(android.R.id.content)).getChildAt(0).
            post(()->onActivityResult2(requestCodeX, resultCodeX, resultDataX));
        }
*/
    }
/*
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Log.d(TAG,"onActivityResult");
//        getView().post(()->onActivityResult2(requestCode, resultCode, resultData));
        requestCodeX=requestCode;
        resultCodeX=resultCode;
        resultDataX=resultData;
    }
*/

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Log.d(TAG, "onActivityResult");

        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri = resultData.getData();  // Выбрали файл
                Log.i(TAG, "Uri: " + uri.toString());

                DoInBackground.run(curActivity, () -> {
                    Entities.ExternalFileDescriptor efd = new Entities.ExternalFileDescriptor(uri);
                    if (efd.externalCode != null) { // full path не null - отправляем
                        String id = cr.uploadFile(efd.externalCode, efd.name);  // ToDo Пока грузится - черный экран
                        if (id != null) {
                            efd.id = id;
                            collectionItems.add(collectionItems.size() - 1, efd); // Предпоследний !
                            curActivity.runOnUiThread(() ->
                            {
                                ((ArrayAdapter<Object>) listView.getAdapter()).notifyDataSetChanged();
                            });
                        } else {
                            message("Ошибка при загрузке файла на сервер\n" + cr.error);
                        }
                    } else {
                        message("Не могу добавить файл\n" + uri.toString());
                    }
                });
            }
        }
    }

    void showList() {
        Log.d(TAG, "showcollectionItemList");

        if (isFragmentNewIstance) {
            collectionItems = ef.value == null ?
                    new ArrayList<>() :
                    new ArrayList<>((ArrayList<Object>) ef.value);  // Копия!
            collectionItems.add("");
        }
        isFragmentNewIstance = false;

        ArrayAdapter<Object> adapter = new collectionItemsArrayAdapter(getActivity(), collectionItems);
        listView = view.findViewById(R.id.listViewCollectionItems);
        listView.setAdapter(adapter);
/*  Since you have a custom adapter, you need to have the onClickListener inside of getView() :(
        listView.setOnItemClickListener( // Не setOnItemSelectedListener !
                (AdapterView<?> parent, View view, int position, long id) -> {
                    viewItemContents((Entities.ExternalFileDescriptor) parent.getAdapter().getItem(position));
                });
*/

    }
}
