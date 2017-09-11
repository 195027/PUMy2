package com.example.mixis.androidprochatapp.Holder;

import com.quickblox.chat.model.QBChatMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Darek on 06.07.2017.
 */

public class QBChatMessagesHolder {
    private static QBChatMessagesHolder instance;
    private HashMap<String,ArrayList<QBChatMessage>>qbChatMessageArray;

    public static synchronized QBChatMessagesHolder getInstance() {
        QBChatMessagesHolder qbChatMessagesHolder;
        synchronized (QBChatMessagesHolder.class)
        {
            if (instance == null)
                instance = new QBChatMessagesHolder();
            qbChatMessagesHolder = instance;

        }
        return qbChatMessagesHolder;
    }

    private QBChatMessagesHolder()
    {
        this.qbChatMessageArray = new HashMap<>();

    }
    public void putMessages(String dialogId,ArrayList<QBChatMessage>qbChatMessages)
    {
        this.qbChatMessageArray.put(dialogId,qbChatMessages);
    }

    public void putMessage(String dialogId,QBChatMessage qbChatMessage)
    {
        List<QBChatMessage> lstRestult = (List)this.qbChatMessageArray.get(dialogId);
        lstRestult.add(qbChatMessage);
        ArrayList<QBChatMessage>lstAdded = new ArrayList<>(lstRestult.size());
        lstAdded.addAll(lstRestult);
        putMessages(dialogId,lstAdded);

    }

    public ArrayList<QBChatMessage>getChatMessageByDialogId(String dialogId)
    {
        return (ArrayList<QBChatMessage>)this.qbChatMessageArray.get(dialogId);
    }



}
