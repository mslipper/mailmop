package com.mslipper.mailmop.service.email.gmail;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.mslipper.mailmop.Util;
import com.mslipper.mailmop.domain.MailmopMessage;
import com.mslipper.mailmop.domain.event.MessagesDeletedEvent;
import com.mslipper.mailmop.domain.event.MessagesFetchedEvent;
import com.mslipper.mailmop.domain.event.ProgressListener;
import com.mslipper.mailmop.inject.ForkJoin;
import com.mslipper.mailmop.service.ForkJoinPoolTaskExecutor;
import com.mslipper.mailmop.service.TaskExecutor;
import com.mslipper.mailmop.service.email.EmailService;
import com.mslipper.mailmop.service.email.gmail.event.AuthorizationEvent;
import javafx.application.Application;
import javafx.application.Application.Parameters;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Singleton
public class GmailService implements EmailService {
    private static final String APPLICATION_NAME = "MailMop";

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);

    private final TaskExecutor taskExecutor;

    private final File gmailConfigFile;

    private final int maxMessages;

    private static HttpTransport HTTP_TRANSPORT;

    private Gmail gmail;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    @Inject
    public GmailService(Application application,
                        @ForkJoin TaskExecutor taskExecutor) {
        Map<String, String> paramsMap = application.getParameters().getNamed();
        maxMessages = paramsMap.containsKey("maxMessages") ?
            Integer.parseInt(paramsMap.get("maxMessages")) : 100000;

        this.taskExecutor = taskExecutor;
        this.gmailConfigFile = Util.loadResourceAsFile("client_id.json");
    }

    @Override
    public synchronized void authorize(AuthorizationListener listener) {
        try {
            Credential credential = doAuthorize(listener);
            gmail = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
        } catch (IOException e) {
            listener.onFailure(e);
        }
    }

    @Override
    public synchronized void fetchMessages(ProgressListener<MessagesFetchedEvent> listener) {
        if (gmail == null) {
            throw new IllegalStateException("Cannot fetch messages without first authenticating with GMail.");
        }

        MessageFetcher fetcher = new MessageFetcher(gmail, (ForkJoinPoolTaskExecutor) taskExecutor, maxMessages);
        fetcher.fetch(listener);
    }

    @Override
    public void deleteMessages(List<MailmopMessage> messages, ProgressListener<MessagesDeletedEvent> listener) {
        MessageDeleter deleter = new MessageDeleter(gmail);
        deleter.delete(messages, listener);
    }

    private Credential doAuthorize(AuthorizationListener listener) throws IOException {
        DataStoreFactory dataStoreFactory = new FileDataStoreFactory(
            Files.createTempDirectory("mailMop").toFile());

        InputStream in = new FileInputStream(gmailConfigFile);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow =
            new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(dataStoreFactory)
                .setAccessType("offline")
                .setCredentialCreatedListener((cred, res) -> listener.onSuccess(new AuthorizationEvent(cred, res)))
                .build();

        return new NotifyingAuthorizationCodeInstalledApp(
            flow, new LocalServerReceiver(), listener)
            .authorize("user");
    }

    private class NotifyingAuthorizationCodeInstalledApp extends AuthorizationCodeInstalledApp {
        private final AuthorizationListener listener;

        public NotifyingAuthorizationCodeInstalledApp(AuthorizationCodeFlow flow,
                                                      VerificationCodeReceiver receiver,
                                                      AuthorizationListener listener) {
            super(flow, receiver);
            this.listener = listener;
        }

        @Override
        protected void onAuthorization(AuthorizationCodeRequestUrl authorizationUrl) throws IOException {
            listener.onOAuthUrl(authorizationUrl.build());
            super.onAuthorization(authorizationUrl);
        }
    }
}
