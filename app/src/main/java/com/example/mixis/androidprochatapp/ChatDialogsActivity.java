package com.example.mixis.androidprochatapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.mixis.androidprochatapp.Adapter.ChatDialogsAdapters;
import com.example.mixis.androidprochatapp.Common.Common;
import com.example.mixis.androidprochatapp.Holder.QBChatDialogHolder;
import com.example.mixis.androidprochatapp.Holder.QBUnreadMessageHolder;
import com.example.mixis.androidprochatapp.Holder.QBUsersHolder;
import com.quickblox.auth.QBAuth;
import com.quickblox.auth.session.BaseService;
import com.quickblox.auth.session.QBSession;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBIncomingMessagesManager;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.QBSystemMessagesManager;
import com.quickblox.chat.exception.QBChatException;
import com.quickblox.chat.listeners.QBChatDialogMessageListener;
import com.quickblox.chat.listeners.QBSystemMessageListener;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.BaseServiceException;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBRequestGetBuilder;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import org.jivesoftware.smack.chat.Chat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ChatDialogsActivity extends AppCompatActivity implements QBSystemMessageListener,QBChatDialogMessageListener{


    FloatingActionButton floatingActionButton;
    ListView lstChatDialogs;




    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        getMenuInflater().inflate(R.menu.chat_dialog_context_menu,menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();


        switch (item.getItemId())
        {
            case R.id.context_delete_dialog:
                deleteDialog(info.position);
                break;

        }
        return true;
    }

    private void deleteDialog(int index) {
        final QBChatDialog chatDialog =(QBChatDialog)lstChatDialogs.getAdapter().getItem(index);
        QBRestChatService.deleteDialog(chatDialog.getDialogId(),false).performAsync(new QBEntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {
                QBChatDialogHolder.getInstance().removeDialog(chatDialog.getDialogId());
                ChatDialogsAdapters adapter = new ChatDialogsAdapters(getBaseContext(),QBChatDialogHolder.getInstance().getAllChatDialogs());
                lstChatDialogs.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_dialog_menu,menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId())
        {

            case R.id.chat_dialog_menu_user:
                showUserProfile();
                break;
            default:
                break;
        }
        return true;
    }

    private void showUserProfile() {
        Intent intent =new Intent(ChatDialogsActivity.this,UserProfile.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChatDialogs();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_dialogs);

        //add toolbar
        Toolbar toolbar= (Toolbar)findViewById(R.id.chat_dialog_toolbar);
        toolbar.setTitle("Android Pro Chat");

        setSupportActionBar(toolbar);



        createSessionForChat();


        lstChatDialogs = (ListView)findViewById(R.id.lstChatDialogs);
        registerForContextMenu(lstChatDialogs);

        lstChatDialogs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                QBChatDialog qbChatDialog = (QBChatDialog)lstChatDialogs.getAdapter().getItem(position);
                Intent intent = new Intent(ChatDialogsActivity.this,ChatMessageActivity.class);
                intent.putExtra(Common.DIALOG_EXTRA,qbChatDialog);
                startActivity(intent);
            }
        });

        loadChatDialogs();





        floatingActionButton = (FloatingActionButton)findViewById(R.id.chatdialog_adduser);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatDialogsActivity.this,ListUsersActivity.class);
                startActivity(intent);
            }
        });



    }

    private void loadChatDialogs() {
        QBRequestGetBuilder requestBuilder = new QBRequestGetBuilder();
        requestBuilder.setLimit(100);

        QBRestChatService.getChatDialogs(null,requestBuilder).performAsync(new QBEntityCallback<ArrayList<QBChatDialog>>() {
            @Override
            public void onSuccess(ArrayList<QBChatDialog> qbChatDialogs, Bundle bundle) {
                //code later
                QBChatDialogHolder.getInstance().putDialogs(qbChatDialogs);

                // unread setings

                Set<String>setIds = new HashSet<>();
                for(QBChatDialog chatDialog : qbChatDialogs)
                    setIds.add(chatDialog.getDialogId());

                //get message unread

                QBRestChatService.getTotalUnreadMessagesCount(setIds, QBUnreadMessageHolder.getInstance().getBundle()).performAsync(new QBEntityCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer integer, Bundle bundle) {
                        //save to cache
                        QBUnreadMessageHolder.getInstance().setBundle(bundle);

                        //refresh list dialogs

                        ChatDialogsAdapters adapter = new ChatDialogsAdapters(getBaseContext(),QBChatDialogHolder.getInstance().getAllChatDialogs());
                        lstChatDialogs.setAdapter(adapter);
                        adapter.notifyDataSetChanged();



                    }

                    @Override
                    public void onError(QBResponseException e) {

                    }
                });




            }

            @Override
            public void onError(QBResponseException e) {

            }
        });


    }

    private void createSessionForChat() {
        final ProgressDialog mDialog = new ProgressDialog(ChatDialogsActivity.this);
           mDialog.setMessage("Please waiting");
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();

        String user ,password;
        user =getIntent().getStringExtra("user");
        password=getIntent().getStringExtra("password");

        //Load all user and save to cache

        QBUsers.getUsers(null).performAsync(new QBEntityCallback<ArrayList<QBUser>>() {
         @Override
         public void onSuccess(ArrayList<QBUser> qbUsers, Bundle bundle) {
             QBUsersHolder.getInstance().putUsers(qbUsers);
         }

         @Override
         public void onError(QBResponseException e) {

         }
     });

        final QBUser qbUser = new QBUser(user,password);
        QBAuth.createSession(qbUser).performAsync(new QBEntityCallback<QBSession>() {
            @Override
            public void onSuccess(QBSession qbSession, Bundle bundle) {
                qbUser.setId(qbSession.getUserId());
                try {
                    qbUser.setPassword(BaseService.getBaseService().getToken());
                } catch (BaseServiceException e) {
                    e.printStackTrace();
                }

                QBChatService.getInstance().login(qbUser, new QBEntityCallback() {
                    @Override
                    public void onSuccess(Object o, Bundle bundle) {
                        mDialog.dismiss();
                        QBSystemMessagesManager qbSystemMessagesManager = QBChatService.getInstance().getSystemMessagesManager();
                        qbSystemMessagesManager.addSystemMessageListener(ChatDialogsActivity.this);

                        QBIncomingMessagesManager qbIncomingMessagesManager= QBChatService.getInstance().getIncomingMessagesManager();
                        qbIncomingMessagesManager.addDialogMessageListener(ChatDialogsActivity.this);

                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Log.e("ERROR",""+e.getMessage());
                    }
                });

            }

            @Override
            public void onError(QBResponseException e) {

            }
        });
    }

    @Override
    public void processMessage(QBChatMessage qbChatMessage) {
        //put dialog to cache
        //because we send message with conntent dialog id

        QBRestChatService.getChatDialogById(qbChatMessage.getBody()).performAsync(new QBEntityCallback<QBChatDialog>() {
            @Override
            public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                // put to cache

                QBChatDialogHolder.getInstance().putDialog(qbChatDialog);
                ArrayList<QBChatDialog>adapterSource = QBChatDialogHolder.getInstance().getAllChatDialogs();
                ChatDialogsAdapters adapters = new ChatDialogsAdapters(getBaseContext(),adapterSource);
                lstChatDialogs.setAdapter(adapters);
                adapters.notifyDataSetChanged();

            }

            @Override
            public void onError(QBResponseException e) {

            }
        });

    }

    @Override
    public void processError(QBChatException e, QBChatMessage qbChatMessage) {
    Log.e("ERROR",""+e.getMessage());
    }

    @Override
    public void processMessage(String s, QBChatMessage qbChatMessage, Integer integer) {
        loadChatDialogs();
    }

    @Override
    public void processError(String s, QBChatException e, QBChatMessage qbChatMessage, Integer integer) {

    }
}
