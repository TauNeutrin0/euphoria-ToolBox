package bots;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import bots.tools.ConnectMessageEventListener;
import bots.tools.StructuredBot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import euphoria.FileIO;
import euphoria.events.MessageEvent;
import euphoria.events.MessageEventListener;
import euphoria.events.PacketEvent;
import euphoria.events.PacketEventListener;

public class euphoMail extends StructuredBot{
  FileIO dataFile;
  //  !emailadd @TauNeutrin0 AKfycbyaxfE4lR-mVVs8ReJ4SQhchg5vg0Isbo8_XyswXeFPz5X2_-w
  //  !emailadd @Baconchicken42 AKfycbxW5Y8naA3qucnP6zKseTlmdU5VwTLioxKaXcWpWl3vZZC3raQ
  //  !emailadd @DoctorNumberFour AKfycbwwdEO-LL9zzszTaKVoU6q_od48WjINufej3ivc28UMJdAkO0c
  //  !emailadd @==> AKfycby3EtaE4Y6cBffAtt_dj1MWEoQ0lYJFg8TAtFRRercJpUj20VY
  //  !emailadd @Sumairu AKfycbzSDOTZv-XSJGgzZMafbNlJbite13_wiNs8Q8r2NYQPYkF2C1w
  //  !emailadd @JeremyRedFur AKfycbzqkLVHcj8lhbvdJ2MFWLFsRv-u-a9XDSRkj4KTGsbzB6gNOI_I
  JsonObject data;
  
  public euphoMail(StructuredBot parent) {
    super(parent,"euphoMail");
    initConsole();
    dataFile = new FileIO("euphoMail_data");
    
    data = dataFile.getJson();
    if(data.has("users")){
      if(!data.get("users").isJsonArray()){
        data.add("users", new JsonArray());
        try {
          dataFile.setJson(data);
        } catch (IOException e) {
          System.err.println("Could not setup users.");
        }
      }
    } else {
      data.add("users", new JsonArray());
      try {
        dataFile.setJson(data);
      } catch (IOException e) {
        System.err.println("Could not setup users.");
      }
    }
    listeners.add(ConnectMessageEventListener.class,new ConnectMessageEventListener("euphoMail",this,dataFile).connectAll());
    
    if(!isConnected("bots")){
      connectStructureToRoom("bots");
    }
    listeners.add(PacketEventListener.class, new MessageEventListener() {
      public void onSendEvent(MessageEvent evt) {
        //SendEvent data = (SendEvent)evt.getPacket().getData();
        if(evt.getMessage().matches("^!ping(?: @euphoMail)?$")){
          reply(evt,"Pong!",null);
        }
        if(evt.getMessage().matches("^!help @euphoMail$")){
          String helpText = "Hello! This bot will send emails to users who have signed up.\n";
          helpText+="Usage \"!email @user Your message here!\"\n";
          helpText+="Commands:\n";
          helpText+="  !emailadd [@user] [id] - Adds a user (see instructions)\n";
          helpText+="  !emailchange [@user] [id] - Changes the app id for a user\n";
          helpText+="  !emailremove [@user] - Removes a user\n";
          helpText+="  !emaillist - Lists all current users\n";
          helpText+="If you want to be added to this bot, follow the instructions found here:\n";
          helpText+="https://drive.google.com/open?id=0B7uRDc-wgQQ0N3ZlaWNUTDFSek0\n";
          helpText+="If you want to add or remove this bot to/from another room, use \"!sendbot @euphoMail &room\" or \"!removebot @euphoMail &room\".\n";
          helpText+="This bot was made by @TauNeutrin0. It will not be online all the time - yet.";
          reply(evt,helpText,null);
        }
        if(evt.getMessage().matches("^!kill @euphoMail$")){
          reply(evt,"/me is now exiting.",null);
          evt.getRoomConnection().closeConnection("Killed by room user.");
        }
        if(evt.getMessage().matches("^!pause @euphoMail$")){
          reply(evt,"/me has been paused.",null);
          evt.getRoomConnection().pause("euphoMail");
        }
        if(evt.getMessage().matches("^!collapse @euphoMail$")){
          setCollapsed(evt.getRoomConnection().getRoom(), true);
          reply(evt,"/me has been collapsed.",null);
        }
        if(evt.getMessage().matches("^!expand @euphoMail$")){
          setCollapsed(evt.getRoomConnection().getRoom(), false);
          reply(evt,"/me has been expanded.",null);
        }
      }
      public void onSnapshotEvent(PacketEvent evt) {}
      public void onHelloEvent(PacketEvent evt) {
        evt.getRoomConnection().changeNick("euphoMail");
      }
      public void onNickEvent(PacketEvent evt) {}
      public void onJoinEvent(PacketEvent evt) {}
      public void onPartEvent(PacketEvent evt) {}
    });
    
    
    listeners.add(PacketEventListener.class, new MessageEventListener(){
        @Override
        public void onSendEvent(MessageEvent evt) {
          String msg = evt.getMessage().replaceAll("[\u0000-\u001f]", "");
          if(msg.matches("^!email @[\\S]+ [\\s\\S]+$")){
            Pattern r = Pattern.compile("^!email @([\\S]+) ([\\s\\S]+)$");
            Matcher m = r.matcher(msg);
            if (m.find()) {
              for(int i=0;i<data.getAsJsonArray("users").size();i++) {
                String nick = data.getAsJsonArray("users").get(i).getAsJsonObject().get("nick").getAsString();
                String appId = data.getAsJsonArray("users").get(i).getAsJsonObject().get("id").getAsString();
                if(m.group(1).equals(nick)) {
                  reply(evt,"Generating link...",null);
                  try {
                    String key = Integer.toString((int) System.currentTimeMillis()%(1000*60*60), 36);
                    key = key + new String(new char[5-key.length()]).replace("\0", "0");
                    System.out.println("Key generated: "+key);
                    if(sendPost(m.group(1), key, appId)) {
                      reply(evt,"Click this link to send your email to @"+m.group(1)+": \nhttps://script.google.com/macros/s/"+appId+"/exec?message="+URLEncoder.encode(m.group(2)+"\n", "UTF-8")+"&sender="+URLEncoder.encode(evt.getSender(), "UTF-8")+"&key="+key,null);
                      System.out.println("Email link generated for @"+m.group(1)+" in &"+evt.getRoomConnection().getRoom()+".");
                    } else {
                      reply(evt,"Failed to get link.",null);
                      System.out.println("Failed to set key.");
                    }
                  } catch (UnsupportedEncodingException e) {
                      e.printStackTrace();
                  } catch (IOException e) {
                    e.printStackTrace();
                  } catch (UnexpectedResponseException e) {
                    e.printStackTrace();
                  }
                }
              }
            }
          }
          
          
          else if(msg.matches("^!emailadd @[\\S]+ [\\S]+$")) {
            Pattern r = Pattern.compile("^!emailadd @([\\S]+) ([\\S]+)$");
            Matcher m = r.matcher(msg);
            if (m.find()) {
              boolean isAdded = false;
              for(int i=0;i<data.getAsJsonArray("users").size();i++){
                if(data.getAsJsonArray("users").get(i).getAsJsonObject().get("nick").getAsString().equals(m.group(1))){
                  isAdded = true;
                }
              }
              if(isAdded){
                reply(evt,"@"+m.group(1)+" is already added. Try using \"!emailchange [@user] [id]\".\nTo check your stored id, use \"!emailcheck [@nick]\".",null);
              } else {
                JsonObject userObject = new JsonObject();
                userObject.addProperty("nick", m.group(1));
                userObject.addProperty("id", m.group(2));
                data.getAsJsonArray("users").add(userObject);
                try {
                  euphoMail.this.dataFile.setJson(data);
                  reply(evt,"@"+m.group(1)+" has been added!",null);
                  System.out.println("@"+m.group(1)+" has been added with id "+m.group(2)+".");
                } catch (IOException e) {
                    e.printStackTrace();
                }
              }
              
            }
          }
          
          
          else if(msg.matches("^!emailremove @[\\S]+$")) {
            Pattern r = Pattern.compile("^!emailremove @([\\S]+)$");
            Matcher m = r.matcher(msg);
            if (m.find()) {
              boolean isRemoved = false;
              for(int i=0;i<dataFile.getJson().getAsJsonArray("users").size();i++){
                if(data.getAsJsonArray("users").get(i).getAsJsonObject().get("nick").getAsString().equals(m.group(1))){
                  isRemoved = true;
                  data.getAsJsonArray("users").remove(i);
                  try {
                    euphoMail.this.dataFile.setJson(data);
                    reply(evt,"@"+m.group(1)+" has been removed.",null);
                    System.out.println("@"+m.group(1)+" has been removed.");
                  } catch (IOException e) {
                      e.printStackTrace();
                  }
                }
              }
              if(!isRemoved){
                reply(evt,"Could not find @"+m.group(1)+".",null);
              }
              
            }
          }
          
          
          else if(msg.matches("^!emailcheck @[\\S]+$")) {
            Pattern r = Pattern.compile("^!emailcheck @([\\S]+)$");
            Matcher m = r.matcher(msg);
            if (m.find()) {
              boolean isAdded = false;
              for(int i=0;i<dataFile.getJson().getAsJsonArray("users").size();i++){
                if(data.getAsJsonArray("users").get(i).getAsJsonObject().get("nick").getAsString().equals(m.group(1))){
                  isAdded = true;
                  reply(evt,"@"+m.group(1)+" is added and has id: "+data.getAsJsonArray("users").get(i).getAsJsonObject().get("id").getAsString()+" .",null);
                }
              }
              if(!isAdded){
                reply(evt,"Could not find @"+m.group(1)+".",null);
              }
              
            }
          }
          
          else if(msg.matches("^!emailchange @[\\S]+ [\\S]+$")) {
            Pattern r = Pattern.compile("^!emailchange @([\\S]+) ([\\S]+)$");
            Matcher m = r.matcher(msg);
            if (m.find()) {
              boolean isRemoved = false;
              for(int i=0;i<dataFile.getJson().getAsJsonArray("users").size();i++){
                if(data.getAsJsonArray("users").get(i).getAsJsonObject().get("nick").getAsString().equals(m.group(1))){
                  isRemoved = true;
                  JsonObject userObject = new JsonObject();
                  userObject.addProperty("nick", m.group(1));
                  userObject.addProperty("id", m.group(2));
                  data.getAsJsonArray("users").set(i, userObject);
                  try {
                    euphoMail.this.dataFile.setJson(data);
                    reply(evt,"@"+m.group(1)+" has been updated.",null);
                    System.out.println("@"+m.group(1)+" has been updated with new id "+m.group(2)+".");
                  } catch (IOException e) {
                      e.printStackTrace();
                  }
                }
              }
              if(!isRemoved){
                reply(evt,"Could not find @"+m.group(1)+".",null);
              }
            }
          }
          
          else if(msg.matches("^!emaillist$")) {
            String text = "Current users:\n";
            for(int i=0;i<euphoMail.this.data.getAsJsonArray("users").size();i++) {
              text+="  "+euphoMail.this.data.getAsJsonArray("users").get(i).getAsJsonObject().get("nick").getAsString()+"\n";
            }
            reply(evt,text,null);
          }
        }
    });
  }
  
  private boolean sendPost(String sender, String key, String scriptID) throws UnsupportedEncodingException, IOException, UnexpectedResponseException{

    String urlStr = "https://script.google.com/macros/s/"+scriptID+"/exec?";
    URL url = new URL(urlStr);
    HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

    //add reuqest header
    con.setRequestMethod("POST");

    // Encode url parameters
    String urlParameters = "sender="+URLEncoder.encode(sender,"UTF-8")+"&key="+URLEncoder.encode(key, "UTF-8");

    // Send post request
    con.setDoOutput(true);
    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
    wr.writeBytes(urlParameters);
    wr.flush();
    wr.close();

    int responseCode = con.getResponseCode();

    if (responseCode != 200) {
        throw new UnexpectedResponseException("Cheap Exception: Unexpected response code: "+responseCode);
    }

    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String inputLine;
    StringBuffer response = new StringBuffer();

    while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
    }
    in.close();

    return response.toString().equals("0");
  }
  
  
  class UnexpectedResponseException extends Exception {
    public UnexpectedResponseException(String err) {
      super(err);
    }
  }
}
