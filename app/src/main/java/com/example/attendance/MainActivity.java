package com.example.attendance;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.attendance.databinding.ActivityMainBinding;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;


import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.server.Server;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    String userId = "";
    ArrayList<User> listOfCheckIn = new ArrayList<>();
    ArrayList<TableRow> rows = new ArrayList<>();
    private FirebaseDatabase database;
    private FirebaseStorage storage;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Button startEndBtn = binding.startEndBtn;


        //when button is clicked
        startEndBtn.setOnClickListener(listener -> {
            if(startEndBtn.getText().equals("Begin Attendance")){
                binding.name.setText("PLEASE SCAN OR INPUT YOUR ID");
                binding.data.setVisibility(View.VISIBLE);
                binding.date.setText(LocalDate.now().toString());
                binding.showListBtn.setVisibility(View.VISIBLE);
                startEndBtn.setText("End Attendance");
            } else {
                startEndBtn.setVisibility(View.INVISIBLE);
                startEndBtn.setEnabled(false);
                binding.enterInputId.setVisibility(View.INVISIBLE);
                binding.showListBtn.setEnabled(false);
                binding.showListBtn.setVisibility(View.INVISIBLE);
                binding.userId.setVisibility(View.INVISIBLE);
                binding.patrolPicture.setVisibility(View.INVISIBLE);
                binding.profilePicture.setVisibility(View.INVISIBLE);
                binding.name.setVisibility(View.INVISIBLE);
                binding.patrolName.setVisibility(View.INVISIBLE);
                binding.helloMsg.setText("Day has ended");
                binding.helloMsg.setVisibility(View.VISIBLE);

                //input users in database
                if(listOfCheckIn.size() != 0){
                    database.getReference("Events").child(LocalDate.now().toString()).setValue(User.idList);
                    startDiscordFunctions();

                }
            }
        });

        binding.enterInputId.setOnClickListener(listener -> {
            String userId = binding.userId.getText().toString();
            if(userId != null && !userId.isEmpty()){
                userId = userId.trim();
                checkUserInput(userId);
            }
        });

        //show list
        binding.showListBtn.setOnClickListener(listener -> {
            if(binding.showListBtn.getText().equals("Open List")){
                binding.showListBtn.setText("Close List");
                binding.scrollView.setVisibility(View.VISIBLE);
                binding.enterInputId.setVisibility(View.INVISIBLE);
                binding.userId.setVisibility(View.INVISIBLE);
                binding.name.setVisibility(View.INVISIBLE);
                binding.patrolName.setVisibility(View.INVISIBLE);
                binding.patrolPicture.setVisibility(View.INVISIBLE);
            } else {
                binding.scrollView.setVisibility(View.INVISIBLE);
                binding.showListBtn.setText("Open List");
                binding.data.setVisibility(View.VISIBLE);
                binding.enterInputId.setVisibility(View.VISIBLE);
                binding.userId.setVisibility(View.VISIBLE);
                binding.name.setVisibility(View.VISIBLE);
                binding.patrolName.setVisibility(View.VISIBLE);
                binding.patrolPicture.setVisibility(View.VISIBLE);
            }
        });
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(binding.startEndBtn.getText().equals("End Attendance")){
            if(event.getAction() == KeyEvent.ACTION_UP){
                userId += (char)event.getUnicodeChar();
                if(event.getKeyCode() == KeyEvent.KEYCODE_ENTER){
                    if(userId != ""){
                        userId = userId.trim();
                        checkUserInput(userId);
                        userId = "";
                    }
                }
            }
        }
        return true;
    }

    public void checkUserInput(String userId){
        if(userId != null && !userId.isEmpty()){
            binding.enterInputId.setEnabled(false);
            binding.userId.setEnabled(false);
            binding.userId.setText(userId);
            if(User.idList.contains(userId)){
                binding.name.setText("You already checked in");
                startTimer();
            } else {
                database.getReference("Users").child(userId).get().addOnSuccessListener(user -> {
                System.out.println("New check in");
                    if(user.exists()){
                        binding.name.setText(user.child("name").getValue().toString());
                        binding.patrolName.setText(user.child("patrol").getValue().toString());
                        User newUser = new User(binding.name.getText().toString(), userId, binding.patrolName.getText().toString());
                        if(user.hasChild("discord")){
                            newUser.setDiscordId(user.child("discord").getValue().toString());
                        }
                        listOfCheckIn.add(newUser);
                        addToVisualList(newUser);
                        binding.helloMsg.setVisibility(View.VISIBLE);
                        //check image
                        if(user.hasChild("picture")){
                            StorageReference storageReference = storage.getReference();
                            storageReference.child(user.child("picture").getValue().toString()).getDownloadUrl().addOnSuccessListener(uri -> {
                                Picasso.get().load(uri).into(binding.profilePicture);
                                startTimer();
                            });
                        } else {
                            startTimer();
                        }
                    } else {
                        binding.name.setText("User not found");
                        startTimer();
                    }
                });
            }
        }
    }

    public void addToVisualList(User user){
        TableRow row = new TableRow(this);
        TextView idView = new TextView(this);
        idView.setText(user.getUserId());
        idView.setPadding(8,8,8,8);

        TextView nameView = new TextView(this);
        nameView.setText(user.getName());
        nameView.setPadding(8,8,8,8);

        Button deleteBtn = new Button(this);
        deleteBtn.setText("Delete");
        deleteBtn.setPadding(8,8,8,8);
        deleteBtn.setOnClickListener(listener -> {
            int index = User.idList.indexOf(user.getUserId());
            if(index != -1){
                User.idList.remove(index);
                binding.visualList.removeView(rows.get(index));
                rows.remove(index);
                Toast.makeText(this, user.getName() + " has been removed", Toast.LENGTH_SHORT).show();
                listOfCheckIn.remove(index);
            }
        });

        row.addView(idView);
        row.addView(nameView);
        row.addView(deleteBtn);
        rows.add(row);
        binding.visualList.addView(row);
    }

    public void startTimer(){
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.profilePicture.setImageResource(R.drawable.s313);
                        binding.helloMsg.setVisibility(View.INVISIBLE);
                        binding.name.setText("PLEASE SCAN OR INPUT YOUR ID");
                        binding.patrolPicture.setImageDrawable(null);
                        binding.patrolName.setText("");
                        binding.userId.setText("");
                        if(!binding.userId.isEnabled()){
                            binding.userId.setEnabled(true);
                        }
                        if(!binding.enterInputId.isEnabled()){
                            binding.enterInputId.setEnabled(true);
                        }
                    }
                });
            }
        };
        timer.schedule(task, 5000);
    }


    public void startDiscordFunctions() {
        DiscordApi api = new DiscordApiBuilder().setToken(Keys.token).login().join();
        Server server = api.getServerById(Keys.guildId).get();
        ServerChannel channel = server.getChannelById(Keys.channelId).get();

            String command = "!givescoutbucks 15 Attendance ";

            for (User next:listOfCheckIn){
                if(!next.getDiscordId().equals("")){
                    command += "<@" + next.getDiscordId() + ">";
                    command += " ";
                }
            }
        //send message for points
        channel.asServerTextChannel().get().sendMessage(command);
    }
}