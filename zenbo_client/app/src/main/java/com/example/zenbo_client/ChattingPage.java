package com.example.zenbo_client;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.net.SocketFactory;

public class ChattingPage extends AppCompatActivity {
    private TextView tv_welcome_msg;

    private ListView receive_block;
    private EditText et_sending_msg;
    private Button btn_send;
    private Button btn_leave;

    private String name;
    private String server_ip;
    private String server_port;


    private Handler mMainHandler;

    Thread netThread;
    Thread clientSocketThread;

    Socket serverSocket = null;
    MsgJsonFormatObj msgObj;
    List<String> receivedMsg = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatting_page);
        initViewElement();

        Intent it = this.getIntent();
        Bundle bundle = it.getExtras();
        name = bundle.getString("name");
        server_ip = bundle.getString("ip");
        server_port = bundle.getString("port");
        tv_welcome_msg.setText("Hi! " + name);
        /*
        mMainHandler = new Handler(Looper.myLooper()) {
            Context _package;
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        MsgJsonFormatObj receiveObj = (MsgJsonFormatObj) msg.obj;
                        receivedMsg.add(receiveObj.getMsg_body());
                        ArrayAdapter adapter = new ArrayAdapter(_package, R.layout.msg_box, receivedMsg.toArray());
                        receive_block.setAdapter(adapter);
                        receive_block.setSelection(adapter.getCount() - 1);
                        break;
                }
            }
            private Handler init(Context pac) {
                _package = pac;
                return this;
            }
        }.init(this);*/

        connect(this);

        btn_leave.setOnClickListener(new View.OnClickListener() {
            private Socket _serverSocket;
            @Override
            public void onClick(View v) {
                msgObj.set_isAlive(false);
                Thread sendThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            SendingMsg(_serverSocket, msgObj);
                            if (clientSocketThread.isAlive()) {
                                clientSocketThread.interrupt();
                                //中斷監聽
                            }
                            if (serverSocket.isConnected()) {
                                serverSocket.close();
                                //關掉socket
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                sendThread.start();
                while (sendThread.isAlive()) {
                    //do nothing
                }
                try {
                    Intent it = new Intent();
                    it.setClass(ChattingPage.this, MainActivity.class);
                    startActivity(it);
                } catch (Exception e) {
                    Log.e("Error", e.getMessage());
                }
            }

            private View.OnClickListener init(Socket serverSocket) {
                _serverSocket = serverSocket;
                return this;
            }
        }.init(serverSocket));

        btn_send.setOnClickListener(new View.OnClickListener() {
            private Socket _serverSocket;

            @Override
            public void onClick(View v) {
                msgObj.setMsg_body(et_sending_msg.getText().toString() + "\n");
                Thread sendThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            SendingMsg(_serverSocket, msgObj);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                sendThread.start();
                while (sendThread.isAlive()) {

                }
                sendThread.interrupt();
                et_sending_msg.setText("");
            }

            private View.OnClickListener init(Socket serverSocket) {
                _serverSocket = serverSocket;
                return this;
            }
        }.init(serverSocket));
    }

    private void connect(Context _package) {
        netThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("Connect", "Waitting to connect......");
                    serverSocket = SocketFactory.getDefault().createSocket();
                    SocketAddress remoteaddr = new InetSocketAddress(server_ip, Integer.parseInt(server_port));
                    try {
                        serverSocket.connect(remoteaddr, 5000);
                    } catch (SocketTimeoutException | UnknownHostException se) {
                        Intent it = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putString("errMsg", "This chatting room does not exist");
                        it.putExtras(bundle);
                        it.setClass(ChattingPage.this, MainActivity.class);
                        Log.d("Connect", "Not Connected");
                        startActivity(it);
                    }

                    if (serverSocket.isConnected()) {
                        Log.d("Connect", "Connected");
                        UUID uuid = UUID.randomUUID();
                        msgObj = new MsgJsonFormatObj(uuid.toString(), name, "");
                        SendingMsg(serverSocket, msgObj);
                        clientSocketThread = new Thread(new thread());
                        clientSocketThread.start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        netThread.start();
        while (netThread.isAlive()) {
            //do nothing
        }
    }

    class thread extends Thread implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    try {
                        if (!ShowMsg()) {
                            break;
                            //聊天室還在線
                        }
                    } catch (Exception ex) {
                        System.out.println("Error: " + ex.getMessage());
                        break;
                    }
                }
                //聊天室不在線要做這些事情。
                if (clientSocketThread.isAlive()) {
                    clientSocketThread.interrupt();
                }
                if (serverSocket.isConnected()) {
                    serverSocket.close();
                }
                Intent it = new Intent();
                it.setClass(ChattingPage.this, MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("errMsg", "This chatting room is closed");
                it.putExtras(bundle);
                startActivity(it);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private boolean ShowMsg() {
        try {
            InputStream in = serverSocket.getInputStream();
            RunnableCallable rct = new RunnableCallable(in);
            Thread parsThread = new Thread(rct);
            parsThread.start();
            while (parsThread.isAlive()) {
                //Main Thread do nothing wait for internet thread
            }

            String receiveStr = rct.call();

            JSONObject jsonObject = new JSONObject(receiveStr);


            MsgJsonFormatObj hostMsg = jsonObjToMsgObj(jsonObject);
            if (hostMsg != null && hostMsg.isAlive()) {
                runOnUiThread(new Runnable() {
                    //不用handler就要用這個。
                    MsgJsonFormatObj _hostMsg;
                    @Override
                    public void run() {
                        receivedMsg.add(_hostMsg.getMsg_body());
                        ArrayAdapter adapter = new ArrayAdapter(ChattingPage.this, R.layout.msg_box, receivedMsg.toArray());
                        receive_block.setAdapter(adapter);
                        receive_block.setSelection(adapter.getCount() - 1);
                    }
                    public Runnable init(Context pac, MsgJsonFormatObj hostMsg){
                        _hostMsg = hostMsg;
                        return this;
                    }
                }.init(this, hostMsg));

                if (hostMsg.get_id().equals("")) {
                    return false;
                    //不在線，回傳false給第220行的thread。
                }
                return true;
                //在線，回傳true給第220行的thread。
            } else {
                msgObj.set_isAlive(false);
                SendingMsg(serverSocket, msgObj);

                return false;
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    private static void SendingMsg(Socket socket, MsgJsonFormatObj msgObj) {
        try {
            JSONObject json = new JSONObject(msgObjToMap(msgObj));
            byte[] jsonByte = (json.toString() + "\n").getBytes();
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.write(jsonByte);
            outputStream.flush();
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
        }
    }

    private static MsgJsonFormatObj jsonObjToMsgObj(JSONObject jsonObj) throws JSONException {

        String id = jsonObj.getString("Id");
        String name = jsonObj.getString("Username");
        String msg_body = jsonObj.getString("Msg_body");
        boolean isAlive = Boolean.parseBoolean(jsonObj.getString("IsAlive"));
        MsgJsonFormatObj resultObj = new MsgJsonFormatObj(id, name, msg_body, isAlive);
        JSONArray memberList = jsonObj.getJSONArray("MemberList");
        resultObj.set_memberList(memberList);
        return resultObj;
    }

    private static HashMap<String, String> msgObjToMap(MsgJsonFormatObj msgObj) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Id", msgObj.get_id());
        map.put("Username", msgObj.get_username());
        map.put("Msg_body", msgObj.getMsg_body());
        map.put("IsAlive", msgObj.isAliveStr());
        return map;
    }

    private void initViewElement() {
        tv_welcome_msg = (TextView) findViewById(R.id.tv_chatting_page_welcome_msg);
        receive_block = (ListView) findViewById(R.id.lv_chatting_page_receive_block);
        et_sending_msg = (EditText) findViewById(R.id.et_chatting_page_sending_msg);
        btn_send = (Button) findViewById(R.id.btn_chatting_page_send);
        btn_leave = (Button) findViewById(R.id.btn_chatting_page_btn_leave);
    }

    private static String parseInfo(InputStream in) {
        String str = "";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            str = br.readLine();
            Log.d("parseInfo() : ", str);
        } catch (IOException e) {
            Log.e("parseInfo Error", e.getMessage());
            e.printStackTrace();
        }
        return str;
    }

    class RunnableCallable implements Callable<String>, Runnable {
        InputStream _in;
        String resultStr;

        @Override
        public void run() {
            resultStr = parseInfo(_in);
        }

        public RunnableCallable(InputStream in) {
            _in = in;
        }

        @Override
        public String call() {
            return resultStr;
        }
    }

}

