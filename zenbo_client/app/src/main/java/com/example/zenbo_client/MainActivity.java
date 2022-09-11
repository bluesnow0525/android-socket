package com.example.zenbo_client;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {
    private EditText name;
    private EditText server_ip;
    private EditText server_port;


    private Button btn_connect;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent it = this.getIntent();
        if(it != null){
            Bundle bundle = it.getExtras();
            if(bundle != null) {
                String errMsg = bundle.getString("errMsg");
                if(errMsg != null && !errMsg.equals("")) {
                    Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show();
                }
            }
        }

        initViewElement();
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putString("name", name.getText().toString());
                bundle.putString("ip", server_ip.getText().toString());
                bundle.putString("port", server_port.getText().toString());

                Intent it = new Intent() ;
                it.putExtras(bundle);
                it.setClass(MainActivity.this , ChattingPage.class ) ;
                startActivity(it) ;
            }
        });

    }

    private void initViewElement(){
        name = (EditText)findViewById(R.id.et_connect_page_name);
        server_ip = (EditText)findViewById(R.id.et_connect_page_ip);
        server_port = (EditText)findViewById(R.id.et_connect_page_port);
        btn_connect = (Button) findViewById(R.id.btn_connect_page_connect);
    }

}