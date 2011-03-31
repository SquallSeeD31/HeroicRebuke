package com.herocraftonline.squallseed31.heroicrebuke;

public class Warning
{
  private Integer id;
  private String to;
  private String from;
  private String message;
  private boolean ack;
  private Long send_time;
  private Long ack_time;
  private String code;

  Warning(Integer id, String to, String from, String message, boolean ack, Long send_time, Long ack_time, String code) {
	this.id = id;
	this.to = to;
    this.from = from;
    this.message = message;
    this.ack = ack;
    this.send_time = send_time;
    this.ack_time = ack_time;
    this.code = code;
  }
  
  Warning(String to, String from, String message)
  {
	  this(null,to,from,message,false,System.currentTimeMillis(),null,null);
  }

  public Integer getId()
  {
	  return this.id;
  }
  
  public String getSender()
  {
    return this.from;
  }

  public String getTarget()
  {
	  return this.to;
  }
  
  public String getMessage()
  {
    return this.message;
  }
  
  public boolean isAcknowledged()
  {
	  return this.ack;
  }
  
  public Long getSendTime()
  {
	  return this.send_time;
  }
  
  public Long getAckTime()
  {
	  return this.ack_time;
  }
  
  public String getCode()
  {
	  return this.code;
  }

  public String toString()
  {
    return this.id + ":" + this.from + ":" + this.to + ":" + this.message + ":" + this.ack + ":" + this.send_time + ":" + this.ack_time + ":" + this.code;
  }

  public void setId(Integer id) {
	  this.id = id;
  }
  
  public void setTo(String to) {
	  this.to = to;
  }
  
  public void setFrom(String from) {
	  this.from = from;
  }
  
  public void setMessage(String message) {
	  this.message = message;
  }
  
  public void setAck() {
	  this.ack = true;
	  this.ack_time = System.currentTimeMillis();
  }

  public void setCode(String code) {
	  this.code = code;
  }
}