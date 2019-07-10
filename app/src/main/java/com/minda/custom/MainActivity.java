package com.minda.custom;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.minda.custom.widget.DampingLayout;

public class MainActivity extends AppCompatActivity {

    public DampingLayout mRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        FragmentTransaction beginTransaction = getFragmentManager().beginTransaction();
        beginTransaction.add(R.id.web, new WebFragment()).commit();
    }

    public void onClick2(View view) {
        mRoot.scrollOpen();
    }

    private void initView() {
        mRoot = (DampingLayout) findViewById(R.id.root);
    }

}
