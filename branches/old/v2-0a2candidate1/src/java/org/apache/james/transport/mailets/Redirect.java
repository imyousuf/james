/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */

package org.apache.james.transport.mailets;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.mailet.*;
import org.apache.james.util.RFC822Date;

/**
*<P>A mailet providing configurable redirection services<BR>
*This mailet can produce listserver, forward and notify behaviour, with the original
*message intact, attached, appended or left out altogether.<BR>
*This built in functionality is controlled by the configuration as laid out below.</P>
*<P>However it is also designed to be easily subclassed to make authoring redirection
*mailets simple. <BR>
*By extending it and overriding one or more of these methods new behaviour can
*be quickly created without the author having to address any other issue than
*the relevant one:</P>
*<UL>
*<LI>attachError() , should error messages be appended to the message</LI>
*<LI>getAttachementType(), what should be attached to the message</LI>
*<LI>getInLineType(), what should be included in the message</LI>
*<LI>getMessage(), The text of the message itself</LI>
*<LI>getRecipients(), the recipients the mail is sent to</LI>
*<LI>getReplyTo(), where replys to this message will be sent</LI>
*<LI>getSender(), who the mail is from</LI>
*<LI>getSubjectPrefix(), a prefix to be added to the message subject</LI>
*<LI>getTo(), a list of people to whom the mail is *apparently* sent</LI>
*<LI>getPassThrough(), should this mailet GHOST the original message.</LI>
*<LI>isStatic(), should this mailet run the get methods for every mail, or just
*once. </LI>
*</UL>
*<P>The configuration parameters are:</P>
*<TABLE width="75%" border="0" cellspacing="2" cellpadding="2">
*<TR>
*<TD width="20%">&lt;recipients&gt;</TD>
*<TD width="80%">A comma delimited list of email addresses for recipients of
*this message, it will use the &quot;to&quot; list if not specified. These
*addresses will only appear in the To: header if no &quot;to&quot; list is
*supplied.</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;to&gt;</TD>
*<TD width="80%">A comma delimited list of addresses to appear in the To: header,
*the email will only be delivered to these addresses if they are in the recipients
*list.<BR>
*The recipients list will be used if this is not supplied</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;sender&gt;</TD>
*<TD width="80%">A single email address to appear in the From: header <BR>
*It can include constants &quot;sender&quot; and &quot;postmaster&quot;</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;message&gt;</TD>
*<TD width="80%">A text message to be the body of the email. Can be omitted.</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;inline&gt;</TD>
*<TD width="80%">
*<P>One of the following items:</P>
*<UL>
*<LI>unaltered &nbsp;&nbsp;&nbsp;&nbsp;The original message is the new
* message, for forwarding/aliasing</LI>
*<LI>heads&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The
* headers of the original message are appended to the message</LI>
*<LI>body&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The
* body of the original is appended to the new message</LI>
*<LI>all&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Both
* headers and body are appended</LI>
*<LI>none&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Neither
* body nor headers are appended</LI>
*</UL>
*</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;attachment&gt;</TD>
*<TD width="80%">
*<P>One of the following items:</P>
*<UL>
*<LI>heads&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The headers of the original
* are attached as text</LI>
*<LI>body&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The body of the original
* is attached as text</LI>
*<LI>all&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Both
* headers and body are attached as a single text file</LI>
*<LI>none&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Nothing is attached</LI>
*<LI>message &nbsp;The original message is attached as type message/rfc822,
* this means that it can, in many cases, be opened, resent, fw'd, replied
* to etc by email client software.</LI>
*</UL>
*</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;passThrough&gt;</TD>
*<TD width="80%">TRUE or FALSE, if true the original message continues in the
*mailet processor after this mailet is finished. False causes the original
*to be stopped.</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;attachError&gt;</TD>
*<TD width="80%">TRUE or FALSE, if true any error message available to the
*mailet is appended to the message body (except in the case of inline ==
*unaltered)</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;replyto&gt;</TD>
*<TD width="80%">A single email address to appear in the Rely-To: header, can
*also be &quot;sender&quot; or &quot;postmaster&quot;, this header is not
*set if this is omited.</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;prefix&gt;</TD>
*<TD width="80%">An optional subject prefix prepended to the original message
*subject, for example..<BR>
*Undeliverable mail:</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;static&gt;</TD>
*<TD width="80%">
*<P>TRUE or FALSE, if this is true it hints to the mailet that none of the
*parameters are set dynamically, and therefore they can be set once in
*the init method.<BR>
*False tells the mailet to call all the &quot;getters&quot; for every mail
*processed.</P>
*<P>This defaults to false.<BR>
*It should be TRUE in all cases, except where one of the getter methods
*has been overriden to provide dynamic values, such as a listserve which
*might override getRecipients() to get a list from a users repository.</P>
*</TD>
*</TR>
*</TABLE>
*
*<P>Example:</P>
*<P> &lt;mailet match=&quot;RecipientIs=test@localhost&quot; class=&quot;Redirect&quot;&gt;<BR>
*&lt;recipients&gt;x@localhost, y@localhost, z@localhost&lt;/recipients&gt;<BR>
*&lt;to&gt;list@localhost&lt;/to&gt;<BR>
*&lt;sender&gt;owner@localhost&lt;/sender&gt;<BR>
*&lt;message&gt;sent on from James&lt;/message&gt;<BR>
*&lt;inline&gt;unaltered&lt;/inline&gt;<BR>
*&lt;passThrough&gt;FALSE&lt;/passThrough&gt;<BR>
*&lt;replyto&gt;postmaster&lt;/replyto&gt;<BR>
*&lt;prefix&gt;[test mailing]&lt;/prefix&gt;<BR>
*&lt;static&gt;TRUE&lt;/static&gt;<BR>
*&lt;passThrough&gt;FALSE&lt;/passThrough&gt;<BR>
*&lt;/mailet&gt;<BR>
*</P>
*<P>and:</P>
*<P> &lt;mailet match=&quot;All&quot; class=&quot;Redirect&quot;&gt;<BR>
*&lt;recipients&gt;x@localhost&lt;/recipients&gt;<BR>
*&lt;sender&gt;postmaster&lt;/sender&gt;<BR>
*&lt;message&gt;Message marked as spam:<BR>
*&lt;/message&gt;<BR>
*&lt;inline&gt;heads&lt;/inline&gt;<BR>
*&lt;attachment&gt;message&lt;/attachment&gt;<BR>
*&lt;passThrough&gt;FALSE&lt;/passThrough&gt;<BR>
*&lt;attachError&gt;TRUE&lt;/attachError&gt;<BR>
*&lt;replyto&gt;postmaster&lt;/replyto&gt;<BR>
*&lt;prefix&gt;[spam notification]&lt;/prefix&gt;<BR>
*&lt;static&gt;TRUE&lt;/static&gt;<BR>
*&lt;passThrough&gt;FALSE&lt;/passThrough&gt;<BR>
*&lt;/mailet&gt;</P>
 *
 * @author  Danny Angus   <danny@thought.co.uk>
 *
 */

public class Redirect extends GenericMailet {
    private MailAddress sender;
    private MailAddress replyTo;
    private String messageText;
    private InternetAddress[] apparentlyTo;
    private Collection recipients;
    private static int UNALTERED = 0;
    private static int HEADS = 1;
    private static int BODY = 2;
    private static int ALL = 3;
    private static int NONE = 4;
    private static int MESSAGE = 5;


/**
* init will setup static values for sender, recipients, message text, and reply to
* <br> if isStatic() returns true
* it calls getSender(), getReplyTo(), getMessage(), and getRecipients() and getTo()
*
*/
    public void init() throws MessagingException {
        log("redirect init");
        if(isStatic()){
            sender      =  getSender()==null ? getMailetContext().getPostmaster():getSender();
            replyTo     =  getReplyTo()==null ? getMailetContext().getPostmaster():getReplyTo();
            messageText =  getMessage();
            recipients  =  getRecipients();
            apparentlyTo = getTo();
            log("static, sender="+sender+", replyTo="+replyTo+", message="+messageText+" ");
        }

    }


/**
*
* Service does the hard work,and redirects the mail in the form specified
*
*
*/

    public void service(Mail mail) throws MessagingException {

        if(!isStatic()){
            sender      =  getSender();
            replyTo     =  getReplyTo();
            messageText =  getMessage();
            recipients  =  getRecipients();
            apparentlyTo = getTo();
        }
        MimeMessage message = mail.getMessage();
        MimeMessage reply = new MimeMessage(Session.getDefaultInstance(System.getProperties(), null));
        //Create the message
        if (getInLineType()!= UNALTERED){
        log("alter message inline=:"+getInLineType());
            StringWriter sout = new StringWriter();
            PrintWriter out = new PrintWriter(sout, true);

            Enumeration heads = message.getAllHeaderLines();
            String head = "";
            while (heads. hasMoreElements() ){
                head += heads.nextElement().toString()+"\n";
            }
            boolean all = false;
            if(messageText!=null){
                out.println(messageText);
            }
            switch(getInLineType()){
                case 3: //ALL:
                    all = true;
                case 1: //HEADS:
                    out.println("Message Headers:");
                    out.println(head);
                    if(! all){
                        break;
                    }
                case 2: //BODY:
                    out.println("Message:");
                    try{
                        out.println(message.getContent().toString());
                    }catch(Exception e){
                        out.println("body unavailable");
                    }
                    break;

                default:
                case 4: //NONE:
                    break;
            }
            MimeMultipart multipart = new MimeMultipart();
            //Add message as the first mime body part
            MimeBodyPart part = new MimeBodyPart();
            part.setText(sout.toString());
            part.setDisposition("inline");
            multipart.addBodyPart(part);
            if(getAttachmentType()!= NONE){
                part = new MimeBodyPart();
                switch(getAttachmentType()){
                    case 1: //HEADS:
                        part.setText(head);
                        break;
                    case 2: //BODY:
                        try{
                            part.setText(message.getContent().toString());
                        }catch(Exception e){
                            part.setText("body unavailable");
                        }
                        break;
                    case 3: //ALL:
                        part.setText(head +"\n\n"+ message.toString());
                        break;
                    case 5: //MESSAGE:
                        part.setContent(message,"message/rfc822");
                        break;

                }
                part.setDisposition("Attachment");
                multipart.addBodyPart(part);

            }
            reply.setContent(multipart);
            reply.setHeader("Content-Type", multipart.getContentType());

        }else{
            log("message resent unaltered:");
            reply=message;
        }


        //Set additional headers

        reply.setSubject(getSubjectPrefix()+message.getSubject());
        if (reply.getHeader("Date")==null){
            reply.setHeader("Date",new RFC822Date().toString());
        }
        reply.setRecipients(Message.RecipientType.TO, apparentlyTo);
        if (replyTo != null){
            InternetAddress[] iart = new InternetAddress[1];
            iart[0]=replyTo.toInternetAddress();
            reply.setReplyTo(iart);
        }
        if(sender == null){
            reply.setHeader("From",message.getHeader("From",","));
        }else{
            reply.setFrom(sender.toInternetAddress());
        }
        //Send it off...
        getMailetContext().sendMail(sender,recipients,reply);

        if(! getPassThrough()){
            mail.setState(Mail.GHOST);
        }
    }


    public String getMailetInfo() {
           return "Resend Mailet";
    }
/**
* must return a Collection of recipient MailAddress'es
*/


    public Collection getRecipients(){
        Collection newRecipients = new HashSet();
        String addressList = getInitParameter("recipients")==null? getInitParameter("to"):getInitParameter("recipients");
        StringTokenizer st = new StringTokenizer(addressList, ",", false);
        while (st.hasMoreTokens()) {
            try{
                newRecipients.add(new MailAddress(st.nextToken()));
            }catch(Exception e){
                log("add recipient failed in getRecipients");
            }
        }

        return newRecipients;
    }


/**
* must return either an empty string, or a message to which the redirect can be attached/appended
*/
    public String getMessage(){
        if(getInitParameter("message")==null){
            return "";
        }else{
            return getInitParameter("message");
        }
    }

/**
* returns an array of InternetAddress 'es for the To: header
*/
    public InternetAddress[] getTo(){
        String addressList = getInitParameter("to")==null? getInitParameter("recipients"):getInitParameter("to");
        StringTokenizer rec = new StringTokenizer(addressList,",") ;
        int tokensn = rec.countTokens();
        InternetAddress[] iaarray = new InternetAddress[tokensn];
        String tokenx ="";
        for( int i=0;i<tokensn;++i){
            try{
                tokenx=rec.nextToken();
                iaarray[i]=new InternetAddress(tokenx);
            }catch(Exception e){
                log("Internet address exception in getTo()");
            }
        }
        return iaarray;
    }


/**
* returns the senders address, as a MailAddress
*/
     public MailAddress getSender(){
         String sr = getInitParameter("sender");
        if(sr != null){
            MailAddress rv;
            if(sr.compareTo("postmaster")==0){
                rv = getMailetContext().getPostmaster();
                return rv;
            }
            if(sr.compareTo("sender")==0){
                return null;
            }
            try{
                rv = new MailAddress(sr);
                return rv;
            }catch(Exception e){
                log("Parse error in getSender "+sr);
            }
         }
         return null;
     }

/**
* returns one of these values to indicate how to append the original message
*<ul>
*    <li>UNALTERED : original message is the new message body</li>
*    <li>BODY : original message body is appended to the new message</li>
*    <li>HEADS : original message headers are appended to the new message</li>
*    <li>ALL : original is appended with all headers</li>
*    <li>NONE : original is not appended</li>
*</ul>
*/
    public int getInLineType(){
        if(getInitParameter("inline")==null){
            return BODY;
        }else{
            return getTypeCode(getInitParameter("inline"));
        }
    }
/**
*     returns one of these values to indicate how to attach the original message
*<ul>
*    <li>BODY : original message body is attached as plain text to the new message</li>
*    <li>HEADS : original message headers are attached as plain text to the new message</li>
*    <li>ALL : original is attached as plain text with all headers</li>
*    <li>MESSAGE : original message is attached as type message/rfc822, a complete mail message.</li>
*    <li>NONE : original is not attached</li>
*</ul>
*
*/
    public int getAttachmentType(){
        if(getInitParameter("attachment")==null){
            return NONE;
        }else{
            return getTypeCode(getInitParameter("attachment"));
        }
    }
/**
* return true to allow thie original message to continue through the processor, false to GHOST it
*/
    public boolean getPassThrough(){
        if(getInitParameter("passThrough")==null){
            return true;
        }else{
            return new Boolean(getInitParameter("passThrough")).booleanValue();
        }
    }
/**
* return true to append a description of any error to the main body part
* if getInlineType does not return "UNALTERED"
*/
    public boolean attachError(){
        if(getInitParameter("attachError")==null){
            return false;
        }else{
            return new Boolean(getInitParameter("attachError")).booleanValue();
        }
    }
/**
* return the reply to address as a string
*/
public MailAddress getReplyTo(){
         String sr = getInitParameter("replyto");
        if(sr != null){
            MailAddress rv;
            if(sr.compareTo("postmaster")==0){
                rv = getMailetContext().getPostmaster();
                return rv;
            }
            if(sr.compareTo("sender")==0){
                return null;
            }
            try{
                rv = new MailAddress(sr);
                return rv;
            }catch(Exception e){
                log("Parse error in getReplyTo "+sr);
            }
         }
         return null;
     }


/**
* return a prefix for the message subject
*/
    public String getSubjectPrefix(){
        if(getInitParameter("prefix")==null){
            return "";
        }else{
            return getInitParameter("prefix");
        }
    }
/**
* return true to reduce calls to getTo, getSender, getRecipients, getReplyTo amd getMessage
* where these values don't change (eg hard coded, or got at startup from the mailet config)<br>
* return false where any of these methods generate their results dynamically eg in response to the message being processed,
* or by refrence to a repository of users
*/
    public boolean isStatic(){
        if(getInitParameter("static")==null){
            return false;
        }
        return new Boolean(getInitParameter("static")).booleanValue();
    }
/**
* A private method to convert types from string to int.
*/
    private int getTypeCode(String param){
        int code;
        param = param.toLowerCase() ;
        if (param.compareTo("unaltered")==0){return 0;}
        if (param.compareTo("heads")==0){return 1;}
        if (param.compareTo("body")==0){return 2;}
        if (param.compareTo("all")==0){return 3;}
        if (param.compareTo("none")==0){return 4;}
        if (param.compareTo("message")==0){return 5;}
        return 4;
    }

}
