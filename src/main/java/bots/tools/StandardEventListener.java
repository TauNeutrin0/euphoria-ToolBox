package bots.tools;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import euphoria.events.MessageEvent;
import euphoria.events.PacketEvent;
import euphoria.events.PacketEventListener;

public class StandardEventListener implements PacketEventListener{
  String nick;
  String helpText;
  StructuredBot bot;
  public StandardEventListener(StructuredBot bot, String nick, String helpText){
    this.bot=bot;
    this.nick=nick;
    this.helpText=helpText;
  }
  @Override
  public void onSendEvent(MessageEvent evt) {
    if(evt.getMessage().matches("^!ping(?: @"+nick+")?$")){
      bot.reply(evt,"Pong!",null);
    }
    if(evt.getMessage().matches("^!help @"+nick+"$")){
      bot.reply(evt,helpText,null);
    }
    if(evt.getMessage().matches("^!kill @"+nick+"$")){
      bot.reply(evt,"/me is now exiting.",null);
      evt.getRoomConnection().closeConnection("Killed by room user.");
    }
    if(evt.getMessage().matches("^!pause @"+nick+"$")){
      bot.reply(evt,"/me has been paused.",null);
      evt.getRoomConnection().pause(nick);
    }
    if(evt.getMessage().matches("^!uptime @"+nick+"$")){
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");
      
      Calendar upDate = bot.getStartupTime();
      long upTime = (new GregorianCalendar(TimeZone.getTimeZone("UTC"))).getTimeInMillis()-upDate.getTimeInMillis();
      String upTimeStr = TimeUnit.MILLISECONDS.toDays(upTime)+"d "+
        TimeUnit.MILLISECONDS.toHours(upTime) % TimeUnit.DAYS.toHours(1)+"h "+
        TimeUnit.MILLISECONDS.toMinutes(upTime) % TimeUnit.HOURS.toMinutes(1)+"m "+
        TimeUnit.MILLISECONDS.toSeconds(upTime) % TimeUnit.MINUTES.toSeconds(1)+"."+
        String.format("%03d", upTime % TimeUnit.SECONDS.toMillis(1))+"s";
      bot.reply(evt,"/me has been up since "+sdf.format(upDate.getTime())+" UTC ("+upTimeStr+").",null);
      
      upDate = evt.getRoomConnection().getStartupTime();
      upTime = (new GregorianCalendar(TimeZone.getTimeZone("UTC"))).getTimeInMillis()-upDate.getTimeInMillis();
      upTimeStr = TimeUnit.MILLISECONDS.toDays(upTime)+"d "+
        TimeUnit.MILLISECONDS.toHours(upTime) % TimeUnit.DAYS.toHours(1)+"h "+
        TimeUnit.MILLISECONDS.toMinutes(upTime) % TimeUnit.HOURS.toMinutes(1)+"m "+
        TimeUnit.MILLISECONDS.toSeconds(upTime) % TimeUnit.MINUTES.toSeconds(1)+"."+
        String.format("%03d", upTime % TimeUnit.SECONDS.toMillis(1))+"s";
      bot.reply(evt,"/me has been online in this room since "+sdf.format(upDate.getTime())+" UTC ("+upTimeStr+").",null);
    }
    
    if(evt.getMessage().matches("^!collapse @"+nick+"$")&&bot.parent!=null){
      bot.setCollapsed(evt.getRoomConnection().getRoom(), true);
      bot.reply(evt,"/me has been collapsed.",null);
    }
    if(evt.getMessage().matches("^!expand @"+nick+"$")&&bot.parent!=null){
      bot.setCollapsed(evt.getRoomConnection().getRoom(), false);
      bot.reply(evt,"/me has been expanded.",null);
    }
  }
  @Override
  public void onSnapshotEvent(PacketEvent evt) {
    evt.getRoomConnection().changeNick(nick);
  }
  public void onHelloEvent(PacketEvent evt) {}
  @Override
  public void onNickEvent(PacketEvent evt) {}
  @Override
  public void onJoinEvent(PacketEvent evt) {}
  @Override
  public void onPartEvent(PacketEvent evt) {}
  @Override
  public void onBounceEvent(PacketEvent arg0) {}
  @Override
  public void packetRecieved(PacketEvent evt) {}
}
