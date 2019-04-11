package com.gf169.smartbills;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import static com.gf169.smartbills.Common.EntityField;
import static com.gf169.smartbills.Common.cr;
import static com.gf169.smartbills.Common.curActivity;
import static com.gf169.smartbills.EditDialogFragment.collectionStr;
import static com.gf169.smartbills.Utils.message;

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
            twV[0].setOnClickListener((View view) -> showItemContents((Entities.ExternalFileDescriptor) collectionItem));

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
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
// ToDo Нужно еще .pdf,.xls,.doc,.docx,.xlsx https://stackoverflow.com/questions/28978581/how-to-make-intent-settype-for-pdf-xlsx-and-txt-file-android
            startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
        } else {
            message("Еще не сделано");
        }
    }

    void showItemContents(Entities.ExternalFileDescriptor efd) {
        if (ef.isAttachments()) {
            String dir = curActivity.getFilesDir().getAbsolutePath();  // Соответствует элементу files-path в provider_paths.xml
            cr.downloadFile(efd.id, dir, null); //efd.name); Будет грузить под одним и тем же именем - downloaded....
            if (cr.responseBodyStr != null) {  // full path
                Intent intent = new Intent(Intent.ACTION_VIEW);
/* https://inthecheesefactory.com/blog/how-to-share-access-to-file-with-fileprovider-on-android-nougat/en
                Uri uri=Uri.parse("file:"+cr.responseBodyStr);
https://developer.android.com/reference/android/support/v4/content/FileProvider
*/
                Uri uri = FileProvider.getUriForFile(curActivity,
                        BuildConfig.APPLICATION_ID + ".provider",
                        new File(cr.responseBodyStr));
                if (uri != null) {
//                    curActivity.grantUriPermission(packageName,uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);  // Именно это!
                    intent.setDataAndType(uri,
                            "image/*"); // Нужно еще .pdf,.xls,.doc,.docx,.xlsx
                    startActivity(intent);
                } else {
                    message("Не могу показать файл\n" + cr.responseBodyStr);
                }
            } else {
                message("Ошибка при загрузке файла c сервера\n" + cr.error);
            }
        } else {
            message("Еще не сделано");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri = resultData.getData();  // Выбрали файл
                Log.i(TAG, "Uri: " + uri.toString());

                Entities.ExternalFileDescriptor efd = new Entities.ExternalFileDescriptor(uri);
                if (efd.externalCode != null) { // full path не null - отправляем
                    String id = cr.uploadFile(efd.externalCode);  // ToDo Пока грузится - черный экран
                    if (id != null) {
                        efd.id = id;
                        collectionItems.add(collectionItems.size() - 1, efd); // Предпоследний !
                        ((ArrayAdapter<Object>) listView.getAdapter()).notifyDataSetChanged();
                    } else {
                        message("Ошибка при загрузке файла на сервер\n" + cr.error);
                    }
                } else {
                    message("Не могу добавить файл\n" + uri.toString());
                }
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
                    showItemContents((Entities.ExternalFileDescriptor) parent.getAdapter().getItem(position));
                });
*/

    }
}
