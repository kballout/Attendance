package com.example.attendance;

import java.util.ArrayList;

public class User {
    String name;
    String userId;
    String discordId = "";
    String patrol;
    public static ArrayList<String> idList = new ArrayList<>();

    public User(String name, String userId, String patrol) {
        this.name = name;
        this.userId = userId;
        this.patrol = patrol;
        idList.add(userId);
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public String getDiscordId() {
        return discordId;
    }

    public String getName() {
        return name;
    }

    public String getPatrol() {
        return patrol;
    }

    public String getUserId() {
        return userId;
    }

    public int removeFromIdList(String id){
        int index = -1;
        index = idList.indexOf(id);
        if(index != -1){
            idList.remove(index);
        }
        return index;
    }

}
