package com.android.chatapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.android.chatapp.model.ChatMessage
import com.android.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*

class ChatLogActivity : AppCompatActivity() {

    companion object{
        val TAG = "ChatLog"
    }

    val adapter = GroupAdapter<ViewHolder>()

    var toUser : User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        recyclerview_chat_log.adapter = adapter

        toUser = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        supportActionBar?.title = toUser?.username
//        setupDummyData()

        listenForMessages()

        send_button.setOnClickListener {
            Log.d(TAG, "Attempt to send message")
            performSendMessage()
        }
    }

    private fun listenForMessages(){
        val fromId = toUser?.uid
        val toId = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId")
        ref.addChildEventListener(object : ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java)
                if (chatMessage != null) {
                    Log.d(TAG, chatMessage.text)

                    if (chatMessage.toId == FirebaseAuth.getInstance().uid){
                        val currentUser = LatestMessagesActivity.currentUser
                        adapter.add(ChatToItem(chatMessage.text,currentUser!!))
                    }else{
                        adapter.add(ChatFromItem(chatMessage.text, toUser!!))
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }


    private fun performSendMessage(){

        val text = editText_chat_log.text.toString()
        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val fromId = toUser?.uid
        val toId = FirebaseAuth.getInstance().uid

        if(fromId == null) return
        if(toId == null) return

//        val reference = FirebaseDatabase.getInstance().getReference("/messages").push()
        val reference = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        val toReference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()

        val chatMessage = ChatMessage(reference.key!!, text, fromId , toId,  System.currentTimeMillis()/1000)
        reference.setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "Saved pur chat message: ${reference.key}")
                editText_chat_log.text.clear()
                recyclerview_chat_log.scrollToPosition(adapter.itemCount-1)
            }

        toReference.setValue(chatMessage)

        val latestMessageRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromId")
        latestMessageRef.setValue(chatMessage)

        val latestMessageToRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId/$toId")
        latestMessageToRef.setValue(chatMessage)


    }
}

class ChatFromItem(val text : String, val user:User):Item<ViewHolder>(){
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.textView_from_row.text = text
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }

}

class ChatToItem(val text: String, val user :User):Item<ViewHolder>(){
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.textView_to_row.text = text
    }

    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }

}