package com.xo.mio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FragmentIn extends Fragment {

    private RelativeLayout relativeLayout;
    private LinearLayout ll1;
    private Button btn;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_in, null);

        relativeLayout =  v.findViewById(R.id.fragmentin_ll);
        ll1 = v.findViewById(R.id.ll1);

        btn = v.findViewById(R.id.btn_add);

        btn.setOnClickListener(new View.OnClickListener( ) {
            @Override
            public void onClick(View v) {
                AddInItem();
            }
        });

        return v;
    }


    public void AddInItem() {
        //LinearLayout linearLayout1 = new LinearLayout(relativeLayout.getContext());
        TextView textView = new TextView(relativeLayout.getContext());
        textView.setText("aaaaa");
        ll1.addView(textView);

    }
}
