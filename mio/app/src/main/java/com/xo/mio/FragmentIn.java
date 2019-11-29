package com.xo.mio;

import android.app.AlertDialog;
import android.app.DirectAction;
import android.content.Context;
import android.content.DialogInterface;
import android.media.CamcorderProfile;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.xml.sax.DTDHandler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FragmentIn extends Fragment {

    private Button btn_add;
    private ListView listView;

    private LinkedList<DataType> linkedListDatas = null;

    private Context mContext;
    private DBOpenHelper dbOpenHelper;
    private MioDataBase mioDataBase;
    private MioAdapterHelper mioAdapterHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_in, null);
        mContext = v.getContext();

        initDB();
        initListViewItem(v);

        return v;
    }



    public void initDB() {
        dbOpenHelper = new DBOpenHelper(mContext, "mio.db", null, 1);
        mioDataBase = new MioDataBase(dbOpenHelper, mContext);
    }


    public void printInfo() {
        if (linkedListDatas.size() > 0) {
            for (DataType dataType : linkedListDatas) {
                Log.e(Debug.TAG, dataType.toString( ));
            }
        }
    }


    private void initListViewItem(View v){

        listView = v.findViewById(R.id.listview);
        registerForContextMenu(listView);


        btn_add = v.findViewById(R.id.btn_add);
        btn_add.setOnClickListener(listenerAdd);

        linkedListDatas = mioDataBase.QueryAllToLinkedList(MioDataBase.TABLE_IN);

        printInfo();

       // if (linkedListDatas.size() > 0) {
            mioAdapterHelper = new MioAdapterHelper(linkedListDatas, mContext);

            listView.setAdapter(mioAdapterHelper);
      //  } else {
        //    Toast.makeText(mContext, "无记录!", Toast.LENGTH_LONG).show();
       // }

    }

    private void RefreshListView() {

        //linkedListDatas = mioDataBase.QueryAllToLinkedList(MioDataBase.TABLE_IN);

        if (linkedListDatas != null) {
            if (mioAdapterHelper == null) {
                mioAdapterHelper = new MioAdapterHelper(linkedListDatas, mContext);
                listView.setAdapter(mioAdapterHelper);
            } else {
                mioAdapterHelper.reFresh(linkedListDatas);
                mioAdapterHelper.notifyDataSetChanged();
            }

        }
    }

    private View.OnClickListener  listenerAdd = new View.OnClickListener( ) {
        @Override
        public void onClick(View v) {
            onAddInItem(v);
        }
    };

    private void onAddInItem(View view) {

        final View addView = View.inflate(mContext, R.layout.activity_diag_addview, null);
        //btn_add = addView.findViewById(R.id.btn_add);
        final EditText et_category = addView.findViewById(R.id.et_category);
        final EditText et_data = addView.findViewById(R.id.et_data);
        final EditText et_note = addView.findViewById(R.id.et_note);


        AlertDialog alertDialog = new AlertDialog.Builder(mContext)
                .setTitle("输入信息")
                .setView(addView)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", new DialogInterface.OnClickListener( ) {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String category, data, note, date;

                        category = et_category.getText().toString();
                        note = et_note.getText().toString();
                        data = et_data.getText().toString();

                        if (!category.isEmpty() && !data.isEmpty()) {

                            DataType dataType = new DataType( );


                            dataType.setId(0);
                            dataType.setCategory(category);
                            dataType.setData(Integer.parseInt(data));
                            dataType.setNote((note.isEmpty()) ? " " : note);

                            Calendar calendar = Calendar.getInstance();
                            int year = calendar.get(Calendar.YEAR);
                            int month = calendar.get(Calendar.MONTH) + 1;
                            int day = calendar.get(Calendar.DAY_OF_MONTH);
                            StringBuffer sb = new StringBuffer(year + "." + month + "." + day);
                            dataType.setDate(sb.toString());

                            mioDataBase.Insert(MioDataBase.TABLE_IN, dataType);
                            linkedListDatas = mioDataBase.QueryAllToLinkedList(MioDataBase.TABLE_IN);

                            mioAdapterHelper.addItem(dataType);

                            //RefreshListView();

                        } else {
                            Toast.makeText(mContext, "请输入完整信息", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .create();

        alertDialog.show();
    }


    private void updateItem(DataType dataType, final int position) {
        final View addView = View.inflate(mContext, R.layout.activity_diag_addview, null);
        final EditText et_category = addView.findViewById(R.id.et_category);
        final EditText et_data = addView.findViewById(R.id.et_data);
        final EditText et_note = addView.findViewById(R.id.et_note);
        final int id = dataType.getId();

        et_category.setText(dataType.getCategory());
        et_data.setText(String.valueOf(dataType.getData()));
        if (!dataType.getNote().isEmpty()) {
            et_note.setText(dataType.getNote());
        }


        AlertDialog alertDialog = new AlertDialog.Builder(mContext)
                .setTitle("输入信息")
                .setView(addView)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", new DialogInterface.OnClickListener( ) {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String category, data, note, date;

                        category = et_category.getText().toString();
                        note = et_note.getText().toString();
                        data = et_data.getText().toString();

                        if (!category.isEmpty() && !data.isEmpty()) {

                            DataType dataType = new DataType( );
                            dataType.setId(id);
                            dataType.setCategory(category);
                            dataType.setData(Integer.parseInt(data));
                            dataType.setNote((note.isEmpty()) ? " " : note);

                            Calendar calendar = Calendar.getInstance();
                            int year = calendar.get(Calendar.YEAR);
                            int month = calendar.get(Calendar.MONTH) + 1;
                            int day = calendar.get(Calendar.DAY_OF_MONTH);
                            StringBuffer sb = new StringBuffer(year + "." + month + "." + day);
                            dataType.setDate(sb.toString());

                            //mioDataBase.Insert(MioDataBase.TABLE_IN, dataType);

                            mioDataBase.UpdateById(MioDataBase.TABLE_IN, dataType);
                            linkedListDatas = mioDataBase.QueryAllToLinkedList(MioDataBase.TABLE_IN);
                            mioAdapterHelper.updateItem(dataType, position);

                            //RefreshListView();

                        } else {
                            Toast.makeText(mContext, "请输入完整信息", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .create();

        alertDialog.show();
    }


    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, @Nullable ContextMenu.ContextMenuInfo menuInfo) {

        MenuInflater inflater = new MenuInflater(mContext);
        inflater.inflate(R.menu.menu, menu);

        super.onCreateContextMenu(menu, v, menuInfo);
    }


    public void deleteItem(DataType dataType) {

        mioDataBase.DeleteById(MioDataBase.TABLE_IN, dataType.getId());
        linkedListDatas = mioDataBase.QueryAllToLinkedList(MioDataBase.TABLE_IN);

        mioAdapterHelper.removeItem(dataType);
       // RefreshListView();
    }


    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        int position = menuInfo.position;
        DataType dataType = (DataType) mioAdapterHelper.getItem(position);
        Log.e("onContextEEE", dataType.toString());


        switch (item.getItemId()) {
            case R.id.menu_update:
                updateItem(dataType, position);
                break;
            case R.id.menu_delete:
                deleteItem(dataType);
                break;
        }

        return true;
    }


}
