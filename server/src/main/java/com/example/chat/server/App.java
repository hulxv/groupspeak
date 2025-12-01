package com.example.chat.server;

public class App {

    public static void main(String[] args) {

        SQLiteDatabase db = new SQLiteDatabase();

        User.initialize(db);
        UserSession.initialize(db);
        Conversation.initialize(db);
        ConversationParticipant.initialize(db);
        ConversationManager.initialize(db);
        Message.initialize(db);

        Server server = new Server(5001);

        Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
            try {
                Thread.sleep(200);
                System.out.printf("\nShutting down ...\n");
                server.shutdown();

            } catch (Exception e) {
                Thread.currentThread().interrupt();
                // e.printStackTrace();
            }
        }
    });

        server.start();

    }
}