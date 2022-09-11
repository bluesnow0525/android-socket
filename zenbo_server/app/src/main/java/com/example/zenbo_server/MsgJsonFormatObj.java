package com.example.zenbo_server;

import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

public class MsgJsonFormatObj {
    private String _id;
    private String _username;
    private String msg_body;
    private boolean _isAlive;
    List<String> _memberList ;

    public MsgJsonFormatObj(String id, String username, String msg, boolean isAlive)
    {
        _id = id;
        _username = username;
        msg_body = msg;
        _isAlive = isAlive;
    }

    public MsgJsonFormatObj(String id, String username, String msg)
    {
        _id = id;
        _username = username;
        msg_body = msg;
        _isAlive = true;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String get_username() {
        return _username;
    }

    public void set_username(String _username) {
        this._username = _username;
    }

    public String getMsg_body() {
        return msg_body;
    }

    public void setMsg_body(String msg_body) {
        this.msg_body = msg_body;
    }

    public String isAliveStr() {

        return Boolean.toString(_isAlive);
    }


    public boolean isAlive() {

        return _isAlive;
    }

    public void set_isAlive(boolean _isAlive) {
        this._isAlive = _isAlive;
    }

    public List<String> getmemberList() {
        return _memberList;
    }

    public void set_memberList(List<String> memberList) {
        this._memberList = memberList;
    }

    public void set_memberList(JSONArray memberList) throws JSONException {
        List<String> result = new ArrayList<String>();
        for(int i =0;i<memberList.length();i++){
            result.add ((String)memberList.get(i));
        }
        this._memberList = result;
    }

}