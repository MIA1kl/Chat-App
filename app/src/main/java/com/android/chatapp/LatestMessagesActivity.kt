package com.android.chatapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.android.chatapp.model.ChatMessage
import com.android.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_latest_messages.*
import kotlinx.android.synthetic.main.latest_message_row.view.*

class LatestMessagesActivity : AppCompatActivity() {

    companion object{
        var currentUser: User? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latest_messages)

        recyclerview_latest_messages.adapter = adapter

        listenForLatestMessages()

        fetchCurrentUser()

        verifyUserIsAuthorized()
    }

    val latestMessagesMap = HashMap<String,ChatMessage>()

    fun refreshRecyclerViewMessages(){
        adapter.clear()
        latestMessagesMap.values.forEach{
            adapter.add(LatestMessageRow(it))
        }
    }

    private fun listenForLatestMessages() {
//        val fromId = toUser?.uid
        val toId = FirebaseAuth.getInstance().uid
        val ref  = FirebaseDatabase.getInstance().getReference("/latest-messages/$toId")
        ref.addChildEventListener(object : ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java) ?: return
                latestMessagesMap[snapshot.key!!] = chatMessage
                refreshRecyclerViewMessages()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java) ?: return
                latestMessagesMap[snapshot.key!!] = chatMessage
                refreshRecyclerViewMessages()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    class LatestMessageRow(val chatMessage: ChatMessage): Item<ViewHolder>(){
        override fun bind(viewHolder: ViewHolder, position: Int) {
            viewHolder.itemView.textView_latest_message.text = chatMessage.text
            val chatPartnerId :String
            if (chatMessage.toId == FirebaseAuth.getInstance().uid ){
                chatPartnerId = chatMessage.fromId
            }else{
                chatPartnerId = chatMessage.toId
            }

            val ref = FirebaseDatabase.getInstance().getReference("/users/$chatPartnerId")
            ref.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    viewHolder.itemView.username_textView_latest_message.text = user?.username
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })

        }

        override fun getLayout(): Int {
            return R.layout.latest_message_row
        }

    }
    val adapter = GroupAdapter<ViewHolder>()


    private fun fetchCurrentUser() {
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        ref.addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser = snapshot.getValue(User::class.java)
                Log.d("LatestMessages", "Current User ${currentUser?.username}")
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun verifyUserIsAuthorized() {
        val uid =FirebaseAuth.getInstance().uid
        if(uid == null){
            val intent = Intent(this,AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item?.itemId){
           R.id.menu_new_message -> {
               val intent = Intent(this,NewMessageActivity::class.java)
               startActivity(intent)
           }
           R.id.menu_sign_out -> {
               FirebaseAuth.getInstance().signOut()
               val intent = Intent(this,AuthActivity::class.java)
               intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
               startActivity(intent)
           }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }
}