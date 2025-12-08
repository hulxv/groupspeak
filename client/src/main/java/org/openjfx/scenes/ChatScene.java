package org.openjfx.scenes;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.openjfx.ChatHandler;
import org.openjfx.ClientState;
import org.openjfx.model.Conversation;
import org.openjfx.model.Message;
import java.io.IOException;
import org.openjfx.Connection;
import org.openjfx.ProtocolHandler;
import org.openjfx.ProtocolHandler.MessageEvent;
import org.openjfx.ProtocolHandler.StatusUpdate;
import org.openjfx.ProtocolHandler.NewConversationEvent;

/**
 * Main Application Class for the "GroupSpeak" Chat Application. This class
 * handles the entire UI setup, event handling, and mock data management. It
 * uses JavaFX for rendering the GUI.
 */
public class ChatScene extends Scene {

    // ==============================================================================================
    //                                         DATA MODELS
    // ==============================================================================================
    /**
     * Represents a single user in the system. Used for listing participants in
     * groups or selecting users for new chats.
     */
    private static class User {

        String id;
        String username;
        String displayName;
        boolean isOnline;

        public User(String id, String username, String displayName, boolean isOnline) {
            this.id = id;
            this.username = username;
            this.displayName = displayName;
            this.isOnline = isOnline;
        }

        @Override
        public String toString() {
            return displayName;
        }

        // Equals and HashCode are crucial for identifying unique users in Lists
        // (e.g., for selection)
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            User user = (User) o;
            return id.equals(user.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    /**
     * Represents the logged-in user's profile details.
     */
    private static class UserProfile {

        String username;
        String email;
        String displayName;
        String status;

        public UserProfile(String username, String email, String displayName,
                String status) {
            this.username = username;
            this.email = email;
            this.displayName = displayName;
            this.status = status;
        }
    }

    /**
     * Represents a conversation thread (either One-to-One or Group). This is
     * the item displayed in the sidebar list.
     */
    private static class Contact {

        String id;
        String name;      // Name of the person or the group
        String colorHex;  // Background color for the avatar
        Image image;      // Optional profile image (currently unused in UI logic)
        int unreadCount;  // Number of unread messages to display as a badge
        boolean isGroup;  // True if this is a group chat
        boolean isOnline; // Only relevant for 1-on-1 chats
        List<User> participants;

        public Contact(String id, String name, String colorHex, Image image,
                boolean isGroup, boolean isOnline) {
            this.id = id;
            this.name = name;
            this.colorHex = colorHex;
            this.image = image;
            this.unreadCount = 0;
            this.isGroup = isGroup;
            this.isOnline = isOnline;
            this.participants = new ArrayList<>();
        }

        /**
         * Helper to get a comma-separated string of participant first names.
         * Used for the subtitle in group chats.
         */
        public String getParticipantNames() {
            if (participants.isEmpty()) {
                return "";
            }
            return participants.stream()
                    .map(u
                            -> u.displayName.split(
                            " ")[0]) // Use first names only for brevity
                    .collect(Collectors.joining(", "));
        }
    }

    // ==============================================================================================
    //                                       UI COMPONENTS
    // ==============================================================================================
    private BorderPane root;      // Main layout container
    private VBox sidebar;         // Left panel containing user list
    private VBox chatView;        // Center panel for active chat
    private VBox placeholderView; // Center panel shown when no chat is selected

    // Chat Area Components
    private VBox chatArea;             // Container for message bubbles
    private ScrollPane chatScrollPane; // Scrollable wrapper for chatArea
    private HBox inputArea; // Bottom area with text field and send button
    private TextField messageInput;
    private VBox contactListContainer; // Container inside the sidebar scroll pane

    // Header Components (Top bar of the active chat)
    private Label currentChatLabel;
    private Label currentChatSubtitle;
    private StackPane currentChatAvatarPane;
    private HBox chatHeaderActions; // Container for action buttons (e.g., Group Info)

    // ==============================================================================================
    //                                          STATE
    // ==============================================================================================
    private final List<Contact> allContacts
            = new ArrayList<>(); // List of all active conversations
    // Removed mockAllUsers usage, will fetch dynamically
    private String currentConversationId = null; // ID of the currently open chat
    private UserProfile myProfile;               // The current user
    private final ChatHandler chatHandler = new ChatHandler();

    public ChatScene() {
        // Call super constructor with a new BorderPane as the root
        super(new BorderPane(), 1100, 750);

        // Initialize the root field from the Scene's root
        this.root = (BorderPane) getRoot();

        // 1. Initialize Data
        loadData();

        // 2. Setup Main Layout
        // root = new BorderPane(); // Removed: root is already initialized via super()
        root.getStyleClass().add("root");

        // 3. Create Key UI Regions
        sidebar = createSidebar();
        chatView = createChatPane();
        placeholderView = createPlaceholderPane();

        // 4. Set Initial View (Sidebar + Placeholder)
        root.setLeft(sidebar);
        root.setCenter(placeholderView);

        // 5. Apply CSS
        URL cssUrl = getClass().getResource("/styles.css");
        if (cssUrl != null) {
            this.getStylesheets().add(cssUrl.toExternalForm());
        }

        // 6. Start Background Thread for incoming messages
        startMockBackgroundListener();
    }

    /**
     * Loads initial data from server.
     */
    private void loadData() {
        ClientState state = ClientState.getInstance();
        myProfile = new UserProfile(
                state.getCurrentUsername(),
                state.getCurrentEmail() != null ? state.getCurrentEmail() : "unknown@example.com",
                state.getCurrentDisplayName() != null ? state.getCurrentDisplayName() : state.getCurrentUsername(),
                "Online"
        );

        try {
            // 1. Fetch all users first to map IDs to User objects
            List<org.openjfx.model.User> modelUsers = chatHandler.getUsers();
            List<User> allUsers = new ArrayList<>();
            for (org.openjfx.model.User mu : modelUsers) {
                allUsers.add(new User(mu.id, mu.username, mu.displayName, mu.isOnline));
            }

            // 2. Load conversations
            List<Conversation> conversations = chatHandler.getConversations();
            String myIdRaw = state.getCurrentUserId();
            String myId_ = (myIdRaw == null) ? "" : myIdRaw.trim();

            for (Conversation conv : conversations) {
                Contact c = new Contact(conv.id, conv.name, getNameColor(conv.name), null, conv.isGroup, false);

                // Populate participants
                for (String pid : conv.participantIds) {
                    // Find user in allUsers
                    for (User u : allUsers) {
                        if (u.id.equals(pid)) {
                            c.participants.add(u);
                            break;
                        }
                    }
                }

                c.participants.removeIf(u -> u.id.equalsIgnoreCase(myId_));

                // Determine online status for 1-on-1
                if (!c.isGroup && !c.participants.isEmpty()) {
                    String myId = state.getCurrentUserId();
                    for (User u : c.participants) {
                        if (!u.id.equals(myId)) {
                            c.isOnline = u.isOnline;
                            break;
                        }
                    }
                }

                allContacts.add(c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Simulates receiving messages from a background thread (server). If chat
     * is open, it adds the message. If not, it increments unread count.
     */
    private void startMockBackgroundListener() {
        ClientState.getInstance().setAsyncMode(true);

        Thread listenerThread = new Thread(() -> {
            try {
                Connection conn = ClientState.getInstance().getConnection();
                while (true) {
                    String line = conn.receive();
                    if (line == null) {
                        break; // Connection closed
                    }
                    // Determine if it's an event or a response
                    if (line.contains("\"type\":\"message\"") && !line.contains("message_response")) {
                        Platform.runLater(() -> handleIncomingMessage(line));
                    } else if (line.contains("\"type\":\"status\"")) {
                        Platform.runLater(() -> handleStatusUpdate(line));
                    } else if (line.contains("\"type\":\"new_conversation\"")) {
                        System.out.println("DEBUG: Received new_conversation event: " + line);
                        Platform.runLater(() -> handleNewConversation(line));
                    } else if (line.contains("\"type\":\"reload_conversations\"")) {
                        System.out.println("DEBUG: Received reload_conversations event: " + line);
                        Platform.runLater(() -> handleReloadConversations(line));
                    } else {
                        // It's a response to a request, put in queue
                        ClientState.getInstance().getResponseQueue().put(line);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handleReloadConversations(String json) {
        new Thread(() -> {
            // Clear the old data and load the new data in the background
            allContacts.clear();
            loadData();

            // Once data is ready, schedule a single UI update on the FX thread
            Platform.runLater(() -> {
                renderContactList(allContacts);
            });
        }).start();
    }

    private void handleNewConversation(String json) {
        System.out.println("DEBUG: New conversation detected, reloading all conversations.");
        handleReloadConversations(json);
    }

    private void handleIncomingMessage(String json) {
        MessageEvent evt = ProtocolHandler.parseMessageEvent(json);
        if (evt.senderId == null) {
            return;
        }
        User sender = null;

        // Check if it's for the current conversation
        if (currentConversationId != null && currentConversationId.equals(evt.conversationId)) {
            // Determine sender name
            String senderName = "Unknown";
            Contact c = allContacts.stream().filter(contact -> contact.id.equals(evt.conversationId)).findFirst().orElse(null);
            if (c != null) {
                for (User u : c.participants) {
                    if (u.id.equals(evt.senderId)) {
                        sender = u;
                        break;
                    }
                }
            }
            if (sender != null) {
                addMessage(evt.content, sender.displayName, sender.id.equals(ClientState.getInstance().getCurrentUserId()));
            }
        } else {
            // Increment unread count
            for (Contact c : allContacts) {
                if (c.id.equals(evt.conversationId)) {
                    c.unreadCount++;
                    // Move to top
                    allContacts.remove(c);
                    allContacts.add(0, c);
                    renderContactList(allContacts);
                    break;
                }
            }
        }
    }

    private void handleStatusUpdate(String json) {
        StatusUpdate status = ProtocolHandler.parseStatusUpdate(json);
        if (status.userId == null) {
            return;
        }

        // Update status in all contacts
        boolean needsRefresh = false;
        for (Contact c : allContacts) {
            // Update participants
            for (User u : c.participants) {
                if (u.id.equals(status.userId)) {
                    u.isOnline = status.isOnline;
                }
            }

            // If 1-on-1, update contact status
            if (!c.isGroup && !c.participants.isEmpty() && c.participants.get(0).id.equals(status.userId)) {
                c.isOnline = status.isOnline;
                needsRefresh = true;

                // Update header if open
                if (currentConversationId != null && currentConversationId.equals(c.id)) {
                    currentChatSubtitle.setText(status.isOnline ? "Online" : "Offline");
                    // Update green dot in header avatar
                    // (Simplified: Re-render header)
                    updateChatHeader(c);
                }
            }
        }
        if (needsRefresh) {
            renderContactList(allContacts);
        }
    }

    // ==============================================================================================
    //                                   CUSTOM UI FACTORIES
    // ==============================================================================================
    /**
     * Creates a custom Cell Factory for ListViews displaying Users. Handles
     * displaying the user's avatar, name, online status, and selection
     * highlighting.
     *
     * * @param isMultiSelect If true, enables robust "click-to-toggle" logic
     * for multi-selection.
     * @return Callback to create ListCells.
     */
    private Callback<ListView<User>, ListCell<User>>
            getUserListCellFactory(boolean isMultiSelect) {
        return listView -> new ListCell<User>() {
            {
                // Listener to update colors immediately when selection changes
                selectedProperty().addListener(
                        (obs, wasSelected, isSelected) -> updateVisualState(isSelected));

                // Custom mouse handler to ensure clicking an item toggles it
                // specifically without clearing other selections (essential for
                // Multi-Select mode)
                if (isMultiSelect) {
                    addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                        if (!isEmpty() && getItem() != null) {
                            MultipleSelectionModel<User> sm
                                    = getListView().getSelectionModel();
                            if (sm.getSelectedIndices().contains(getIndex())) {
                                sm.clearSelection(getIndex());
                            } else {
                                sm.select(getIndex());
                            }
                            event.consume(); // Prevent default behavior which might clear
                            // selection
                        }
                    });
                }
            }

            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    // Build the graphic for the cell
                    HBox box = new HBox(10);
                    box.setAlignment(Pos.CENTER_LEFT);

                    StackPane avatar = createAvatarPane(user.displayName, "#3b82f6", 32,
                            12, user.isOnline);

                    VBox textInfo = new VBox(2);
                    Label name = new Label(user.displayName);
                    name.setStyle("-fx-text-fill: -text-primary; -fx-font-weight: bold;");

                    Label status = new Label(user.isOnline ? "Online" : "Offline");
                    status.setStyle("-fx-text-fill: "
                            + (user.isOnline ? "#22c55e" : "-text-secondary")
                            + "; -fx-font-size: 11px;");

                    textInfo.getChildren().addAll(name, status);
                    box.getChildren().addAll(avatar, textInfo);
                    setGraphic(box);

                    updateVisualState(isSelected());
                }
            }

            // Updates the background and text color based on selection state
            private void updateVisualState(boolean isSelected) {
                if (getItem() == null || getGraphic() == null) {
                    return;
                }

                HBox box = (HBox) getGraphic();
                VBox textInfo = (VBox) box.getChildren().get(1);
                Label name = (Label) textInfo.getChildren().get(0);

                if (isSelected) {
                    setStyle("-fx-background-color: -selected-color;"); // Blue background
                    name.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-background-color: transparent;");
                    name.setStyle("-fx-text-fill: -text-primary; -fx-font-weight: bold;");
                }
            }
        };
    }

    /**
     * Logic to filter users. Checks if any part of their name (First or Last)
     * starts with the search text provided.
     */
    private boolean searchMatches(User user, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return true;
        }
        String[] nameParts
                = user.displayName.split("\\s+"); // Split full name into words
        for (String part : nameParts) {
            if (part.toLowerCase().startsWith(searchText.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // ==============================================================================================
    //                                      DIALOGS
    // ==============================================================================================
    /**
     * Shows a dialog displaying group members. Allows adding new members or
     * removing existing ones.
     */
    private void showGroupInfoDialog(Contact group) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Group Info");
        dialog.setHeaderText(null);

        // Load Styles
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(
                getClass().getResource("/styles.css").toExternalForm());
        if (root.getStyleClass().contains("light-theme")) {
            pane.getStyleClass().add("light-theme");
        }

        pane.getButtonTypes().add(ButtonType.CLOSE);

        // --- Dialog Layout ---
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(420);

        // Header: Avatar + Group Name
        StackPane avatar
                = createAvatarPane(group.name, group.colorHex, 60, 20, false);
        Label nameLabel = new Label(group.name);
        nameLabel.getStyleClass().add("dialog-text");
        nameLabel.setStyle("-fx-font-size: 18px;");

        // --- CHANGED: Count includes "You" (+1) ---
        Label countLabel
                = new Label((group.participants.size() + 1) + " participants");
        countLabel.getStyleClass().add("contact-sub");

        VBox headerBox = new VBox(5, avatar, nameLabel, countLabel);
        headerBox.setAlignment(Pos.CENTER);

        // --- CHANGED: Construct list including "You" with actual Display Name ---
        ListView<User> membersList = new ListView<>();

        // Define a constant ID for the current user within this context to detect
        // self-selection
        final String MY_ID = "CURRENT_USER_ID";

        // --- UPDATED: Display name format "Name (You)" ---
        User me = new User(MY_ID, myProfile.username, myProfile.displayName + " (You)", true);

        // Combine "You" with the actual participants list
        List<User> fullMemberList = new ArrayList<>();
        fullMemberList.add(me);
        fullMemberList.addAll(group.participants);

        membersList.setItems(FXCollections.observableArrayList(fullMemberList));
        membersList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        membersList.setCellFactory(getUserListCellFactory(true));
        membersList.getStyleClass().add("dialog-list-view");
        membersList.setPrefHeight(250);

        // Buttons
        Button addBtn = new Button("Add Participant");
        addBtn.getStyleClass().add("send-btn");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setPrefHeight(40);
        HBox.setHgrow(addBtn, Priority.ALWAYS);

        addBtn.setOnAction(e -> {
            showAddParticipantDialog(group);
            // Refresh list keeping "You" at the top
            List<User> updatedList = new ArrayList<>();
            updatedList.add(me);
            updatedList.addAll(group.participants);
            membersList.setItems(FXCollections.observableArrayList(updatedList));
            countLabel.setText((group.participants.size() + 1) + " participants");
        });

        Button removeBtn = new Button("Remove Selected");
        removeBtn.getStyleClass().add("logout-btn");
        removeBtn.setMaxWidth(Double.MAX_VALUE);
        removeBtn.setPrefHeight(40);
        HBox.setHgrow(removeBtn, Priority.ALWAYS);

        // Disable remove button if nothing is selected
        removeBtn.setDisable(true);
        membersList.getSelectionModel().getSelectedItems().addListener(
                (ListChangeListener<User>) c
                -> removeBtn.setDisable(
                        membersList.getSelectionModel().getSelectedItems().isEmpty()));

        removeBtn.setOnAction(e -> {
            List<User> selected
                    = new ArrayList<>(membersList.getSelectionModel().getSelectedItems());
                    //HERE
            if (!selected.isEmpty()) {
                // --- ADDED: Check if the user is removing themselves (Leaving Group)
                // ---
                boolean removingSelf
                        = selected.stream().anyMatch(u -> u.id.equals(MY_ID));

                if (removingSelf) {
                    // User is leaving the group -> Close dialog and Delete Conversation
                    dialog.setResult(null);
                    dialog.close();
                    deleteConversation(group);
                } else {
                    new Thread(() -> {
                        List<String> removedUserIds = new ArrayList<>();
                        for (User u : selected) {
                            try {
                                chatHandler.removeParticipantFromGroup(group.id, u.id);
                                removedUserIds.add(u.id);
                            } catch (IOException ex) {
                                Platform.runLater(() -> System.err.println("Failed to remove " + u.displayName));
                                ex.printStackTrace();
                            }
                        }
                    }).start();
                }

                    // Refresh list keeping "You" at the top
                    List<User> updatedList = new ArrayList<>();
                    updatedList.add(me);
                    updatedList.addAll(group.participants);
                    membersList.setItems(FXCollections.observableArrayList(updatedList));
                    countLabel.setText((group.participants.size() + 1) + (" participant"
                            + "s"));
                    updateChatHeader(group);
                    removeBtn.setDisable(true);
                }
        });

        HBox buttonBox = new HBox(15, addBtn, removeBtn);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setFillHeight(true);

        content.getChildren().addAll(headerBox, new Separator(), membersList,
                buttonBox);
        pane.setContent(content);

        dialog.showAndWait();
    }

    /**
     * Shows the dialog to create a NEW conversation (One-to-One or Group).
     * Includes search functionality and validation logic.
     */
    private void showNewConversationDialog() {
        Dialog<Contact> dialog = new Dialog<>();
        dialog.setTitle("New Conversation");
        dialog.setHeaderText(null);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(
                getClass().getResource("/styles.css").toExternalForm());
        if (root.getStyleClass().contains("light-theme")) {
            dialogPane.getStyleClass().add("light-theme");
        }

        ButtonType createButtonType
                = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 30, 20, 20));

        // 1. Conversation Type Toggle
        ToggleGroup typeGroup = new ToggleGroup();
        RadioButton rbOneToOne = new RadioButton("One-to-One Conversation");
        rbOneToOne.setToggleGroup(typeGroup);
        rbOneToOne.setSelected(true);
        rbOneToOne.getStyleClass().add("dialog-radio");

        RadioButton rbGroup = new RadioButton("Group Chat");
        rbGroup.setToggleGroup(typeGroup);
        rbGroup.getStyleClass().add("dialog-radio");

        HBox radioBox = new HBox(15, rbOneToOne, rbGroup);

        // 2. Search Field
        TextField userSearchField = new TextField();
        userSearchField.setPromptText("Search online users...");
        userSearchField.getStyleClass().add("search-field");

        // 3. User Selection List
        Label selectUserLabel = new Label("Select Person:");
        selectUserLabel.getStyleClass().add("dialog-text");

        ListView<User> userListView = new ListView<>();
        userListView.setPrefHeight(200);
        userListView.setPrefWidth(300);
        userListView.getStyleClass().add("dialog-list-view");

        // 4. Group Name Input
        Label groupNameLabel = new Label("Group Name:");
        groupNameLabel.getStyleClass().add("dialog-text");
        TextField groupNameField = new TextField();
        groupNameField.setPromptText("Enter group name...");
        groupNameField.getStyleClass().add("search-field");

        // Fetch Users from Server
        List<User> serverUsers = new ArrayList<>();
        try {
            List<org.openjfx.model.User> modelUsers = chatHandler.getUsers();
            String myId = ClientState.getInstance().getCurrentUserId();
            for (org.openjfx.model.User u : modelUsers) {
                if (!u.id.equals(myId)) {
                    serverUsers.add(new User(u.id, u.username, u.displayName, u.isOnline));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // --- Data Binding & Filtering ---
        ObservableList<User> baseList = FXCollections.observableArrayList();
        FilteredList<User> filteredList = new FilteredList<>(baseList, p -> true);
        userListView.setItems(filteredList);

        // Real-time search filtering
        userSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(user -> searchMatches(user, newVal));
        });

        // --- Validation Logic (Enabling "Create" button) ---
        Button createBtn = (Button) dialogPane.lookupButton(createButtonType);
        createBtn.setDisable(true);

        Runnable validate = () -> {
            int selectedCount
                    = userListView.getSelectionModel().getSelectedItems().size();
            if (rbOneToOne.isSelected()) {
                // One-to-One: Must select exactly 1 person
                createBtn.setDisable(selectedCount != 1);
            } else {
                // Group: Must select AT LEAST 2 people (You + 2 others = 3 minimum) AND
                // have a name
                boolean hasName = !groupNameField.getText().trim().isEmpty();
                createBtn.setDisable(selectedCount < 2 || !hasName);
            }
        };

        // Attach listeners to trigger validation
        userListView.getSelectionModel().getSelectedItems().addListener(
                (ListChangeListener<User>) c -> validate.run());
        groupNameField.textProperty().addListener(
                (obs, old, val) -> validate.run());

        // --- Mode Switching Logic (1-on-1 vs Group) ---
        Runnable setupForOneToOne = () -> {
            // Remove group fields
            grid.getChildren().removeAll(groupNameLabel, groupNameField);
            selectUserLabel.setText("Select Person:");

            // Data: Online & Not already chatted with
            List<User> available = serverUsers.stream()
                    .filter(u -> u.isOnline)
                    .filter(u
                            -> allContacts.stream().noneMatch(
                            c -> c.name.equals(u.displayName)))
                    .collect(Collectors.toList());

            baseList.setAll(available);

            userListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            userListView.setCellFactory(getUserListCellFactory(false));
            userListView.getSelectionModel().clearSelection();

            if (available.isEmpty()) {
                userListView.setPlaceholder(new Label("No new online users."));
            }
            validate.run();
        };

        Runnable setupForGroup = () -> {
            // Add group fields if missing
            if (!grid.getChildren().contains(groupNameLabel)) {
                grid.add(groupNameLabel, 0, 1);
                grid.add(groupNameField, 1, 1);
            }
            selectUserLabel.setText("Participants (Online):");

            // Data: All Online users
            List<User> onlineUsers = serverUsers.stream()
                    .filter(u -> u.isOnline)
                    .collect(Collectors.toList());

            baseList.setAll(onlineUsers);

            userListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            userListView.setCellFactory(getUserListCellFactory(true));
            userListView.getSelectionModel().clearSelection();

            if (onlineUsers.isEmpty()) {
                userListView.setPlaceholder(new Label("No online users found."));
            }
            validate.run();
        };

        // Add components to grid
        grid.add(radioBox, 0, 0, 2, 1);
        grid.add(selectUserLabel, 0, 2);
        grid.add(userSearchField, 1, 2); // Search field
        grid.add(userListView, 1, 3);    // User list

        setupForOneToOne.run(); // Initial state

        // Handle Toggle Switch
        rbOneToOne.setOnAction(e -> {
            userSearchField.clear();
            setupForOneToOne.run();
            dialog.getDialogPane().getScene().getWindow().sizeToScene();
        });
        rbGroup.setOnAction(e -> {
            userSearchField.clear();
            setupForGroup.run();
            dialog.getDialogPane().getScene().getWindow().sizeToScene();
        });

        dialogPane.setContent(grid);

        // Result Converter: Turns the dialog result into a Contact object
        dialog.setResultConverter(btn -> {
            if (btn == createButtonType) {
                if (rbOneToOne.isSelected()) {
                    User u = userListView.getSelectionModel().getSelectedItem();
                    if (u != null) {
                        try {
                            Conversation conv = chatHandler.createConversation(u.username, null, null);
                            if (conv != null) {
                                Contact c = new Contact(conv.id, conv.name, getNameColor(conv.name), null, conv.isGroup, true);
                                c.participants.add(u);
                                return c;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    String name = groupNameField.getText();
                    List<User> selectedUsers = userListView.getSelectionModel().getSelectedItems();

                    // Get usernames
                    List<String> usernames = selectedUsers.stream().map(user -> user.username).collect(Collectors.toList());
                    String participantsStr = String.join(",", usernames);

                    try {
                        Conversation conv = chatHandler.createConversation(null, name, participantsStr);

                        if (conv != null) {
                            Contact c = new Contact(conv.id, conv.name, getNameColor(conv.name), null, conv.isGroup, true);
                            c.participants.addAll(selectedUsers);
                            return c;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        });

        Optional<Contact> result = dialog.showAndWait();
        result.ifPresent(contact -> {
            allContacts.add(0, contact); // Add to top
            renderContactList(allContacts);
            openChat(contact);
        });
    }

    /**
     * Shows a dialog to add new participants to an existing group. Filters list
     * to show only ONLINE users who are NOT ALREADY in the group.
     */
    private void showAddParticipantDialog(Contact group) {
        Dialog<List<User>> dialog = new Dialog<>();
        dialog.setTitle("Add Participants");
        dialog.setHeaderText("Add people to " + group.name);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(
                getClass().getResource("/styles.css").toExternalForm());
        if (root.getStyleClass().contains("light-theme")) {
            dialogPane.getStyleClass().add("light-theme");
        }

        ButtonType addButtonType
                = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Fetch Users from Server
        List<User> serverUsers = new ArrayList<>();
        try {
            List<org.openjfx.model.User> modelUsers = chatHandler.getUsers();
            String myId = ClientState.getInstance().getCurrentUserId();
            for (org.openjfx.model.User u : modelUsers) {
                if (!u.id.equals(myId)) {
                    serverUsers.add(new User(u.id, u.username, u.displayName, u.isOnline));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Filter: Online AND Not in group
        List<User> available = serverUsers.stream()
                .filter(u -> u.isOnline)
                .filter(u
                        -> group.participants.stream().noneMatch(
                        p -> p.id.equals(u.id)))
                .collect(Collectors.toList());

        // Search Field
        TextField searchField = new TextField();
        searchField.setPromptText("Search users...");
        searchField.getStyleClass().add("search-field");

        // Filtered List binding
        FilteredList<User> filteredList = new FilteredList<>(
                FXCollections.observableArrayList(available), p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(user -> searchMatches(user, newVal));
        });

        ListView<User> listView = new ListView<>();
        listView.setItems(filteredList);
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setCellFactory(getUserListCellFactory(true));
        listView.setPrefHeight(200);
        listView.getStyleClass().add("dialog-list-view");

        if (available.isEmpty()) {
            listView.setPlaceholder(new Label("No other online users available."));
        }

        VBox content
                = new VBox(10, new Label("Select users:"), searchField, listView);
        content.setPadding(new Insets(20));
        dialogPane.setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == addButtonType) {
                List<User> selected = new ArrayList<>(listView.getSelectionModel().getSelectedItems());
                // Add to server
                for (User u : selected) {
                    try {
                        chatHandler.addParticipantToGroup(group.id, u.id);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return selected;
            }
            return null;
        });

        Optional<List<User>> result = dialog.showAndWait();
        result.ifPresent(users -> {
            if (!users.isEmpty()) {
                group.participants.addAll(users);
                // Refresh header if currently viewing this group
                if (currentConversationId != null
                        && currentConversationId.equals(group.id)) {
                    updateChatHeader(group);
                }
            }
        });
    }

    // ==============================================================================================
    //                                   MAIN UI CONSTRUCTION
    // ==============================================================================================
    /**
     * Builds and shows the User Profile view (replaces the main chat view).
     */
    private void showProfileView() {
        VBox profileView = new VBox(20);
        profileView.setAlignment(Pos.CENTER);
        profileView.getStyleClass().add("chat-pane");
        profileView.setPadding(new Insets(40));

        Button backBtn = new Button("← Back to Chats");
        backBtn.getStyleClass().add("simple-btn");
        backBtn.setOnAction(e -> restoreMainView());

        StackPane bigAvatar
                = createAvatarPane(myProfile.displayName, "#3b82f6", 80, 24, false);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);
        grid.setMaxWidth(500);

        String labelStyle = "-fx-text-fill: -text-secondary; -fx-font-size: 14px;";
        String valueStyle = "-fx-text-fill: -text-primary; -fx-font-size: 16px; "
                + "-fx-font-weight: bold;";

        addProfileRow(grid, 0, "Display Name", myProfile.displayName, labelStyle,
                valueStyle);
        addProfileRow(grid, 1, "Username", "@" + myProfile.username, labelStyle,
                valueStyle);
        addProfileRow(grid, 2, "Email", myProfile.email, labelStyle, valueStyle);

        Label statusLabel = new Label("Status");
        statusLabel.setStyle(labelStyle);
        Label statusValue = new Label(myProfile.status);
        statusValue.setStyle(
                "-fx-text-fill: #22c55e; -fx-font-size: 16px; -fx-font-weight: bold;");
        grid.add(statusLabel, 0, 3);
        grid.add(statusValue, 1, 3);

        profileView.getChildren().addAll(backBtn, bigAvatar, grid);

        root.setLeft(null);
        root.setCenter(profileView);
    }

    private void addProfileRow(GridPane grid, int row, String label, String value,
            String lStyle, String vStyle) {
        Label l = new Label(label);
        l.setStyle(lStyle);
        Label v = new Label(value);
        v.setStyle(vStyle);
        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }

    /**
     * Restores the standard view (Sidebar + Chat) after viewing the profile.
     */
    private void restoreMainView() {
        root.setLeft(sidebar);
        if (currentConversationId != null) {
            root.setCenter(chatView);
        } else {
            root.setCenter(placeholderView);
        }
    }

    /**
     * Creates the Sidebar containing the search bar and the list of
     * conversations.
     */
    private VBox createSidebar() {
        VBox sidebarBox = new VBox();
        sidebarBox.getStyleClass().add("sidebar");
        sidebarBox.setPrefWidth(340);

        // Sidebar Header (Title + Buttons)
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 10, 20));

        Label appTitle = new Label("Chats");
        appTitle.getStyleClass().add("app-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newChatBtn = new Button("+");
        newChatBtn.getStyleClass().add("icon-btn");
        newChatBtn.setTooltip(new Tooltip("New Conversation"));
        newChatBtn.setOnAction(e -> showNewConversationDialog());

        Button profileBtn = new Button("👤");
        profileBtn.getStyleClass().add("icon-btn");
        profileBtn.setTooltip(new Tooltip("My Profile"));
        profileBtn.setOnAction(e -> showProfileView());

        // Theme Switcher Logic
        ToggleButton themeSwitch = new ToggleButton("☀");
        themeSwitch.getStyleClass().add("icon-btn");
        themeSwitch.setOnAction(e -> {
            if (themeSwitch.isSelected()) {
                root.getStyleClass().add("light-theme");
                themeSwitch.setText("☾");
            } else {
                root.getStyleClass().remove("light-theme");
                themeSwitch.setText("☀");
            }
        });

        header.getChildren().addAll(appTitle, spacer, newChatBtn, profileBtn,
                themeSwitch);

        // Sidebar Search Bar
        HBox searchContainer = new HBox();
        searchContainer.setPadding(new Insets(0, 20, 10, 20));
        TextField searchField = new TextField();
        searchField.setPromptText("Search conversations...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefHeight(35);
        searchField.prefWidthProperty().bind(
                sidebarBox.widthProperty().subtract(40));
        searchField.textProperty().addListener(
                (obs, old, newVal) -> filterContacts(newVal));
        searchContainer.getChildren().add(searchField);

        // Contact List Scroll Area
        contactListContainer = new VBox(5);
        contactListContainer.getStyleClass().add("contact-list");
        contactListContainer.setPadding(new Insets(5, 10, 5, 10));

        ScrollPane contactScroll = new ScrollPane(contactListContainer);
        contactScroll.getStyleClass().add("scroll-pane-clean");
        contactScroll.setFitToWidth(true);
        VBox.setVgrow(contactScroll, Priority.ALWAYS);

        // Footer (Logout)
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(15, 20, 15, 20));
        footer.getStyleClass().add("sidebar-footer");

        Button logoutBtn = new Button("Sign Out");
        logoutBtn.getStyleClass().add("logout-btn");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(logoutBtn, Priority.ALWAYS);
        logoutBtn.setOnAction(e -> showLogoutDialog());

        footer.getChildren().add(logoutBtn);

        renderContactList(allContacts);

        sidebarBox.getChildren().addAll(header, searchContainer, contactScroll,
                footer);
        return sidebarBox;
    }

    /**
     * Filters the conversation list in the sidebar.
     */
    private void filterContacts(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            renderContactList(allContacts);
        } else {
            List<Contact> filtered
                    = allContacts.stream()
                            .filter(
                                    c -> c.name.toLowerCase().contains(searchText.toLowerCase()))
                            .collect(Collectors.toList());
            renderContactList(filtered);
        }
    }

    /**
     * Renders the list of conversations (Contacts) into the sidebar.
     */
    private void renderContactList(List<Contact> contactsToDisplay) {
        contactListContainer.getChildren().clear();

        for (Contact contact : contactsToDisplay) {
            HBox contactBox = new HBox(12);
            contactBox.getStyleClass().add("contact-item");
            contactBox.setAlignment(Pos.CENTER_LEFT);
            contactBox.setPadding(new Insets(12, 15, 12, 15));

            StackPane avatarPane = createAvatarPane(contact.name, contact.colorHex,
                    42, 14, contact.isOnline);

            VBox infoBox = new VBox(2);
            Label nameLabel = new Label(contact.name);
            nameLabel.getStyleClass().add("contact-name");
            Label subLabel
                    = new Label(contact.isGroup ? "Group Chat" : "One-to-One Conversation");
            subLabel.getStyleClass().add("contact-sub");
            infoBox.getChildren().addAll(nameLabel, subLabel);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            contactBox.getChildren().addAll(avatarPane, infoBox);

            // Unread Badge
            if (contact.unreadCount > 0) {
                Label unreadBadge = new Label(String.valueOf(contact.unreadCount));
                unreadBadge.getStyleClass().add("unread-badge");
                contactBox.getChildren().add(unreadBadge);
            }

            // Context Menu (Right-click)
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete Conversation");
            deleteItem.setOnAction(e -> deleteConversation(contact));
            contextMenu.getItems().add(deleteItem);

            contactBox.setOnContextMenuRequested(
                    e -> contextMenu.show(contactBox, e.getScreenX(), e.getScreenY()));

            // Click Handler
            contactBox.setOnMouseClicked(e -> openChat(contact));

            if (currentConversationId != null
                    && currentConversationId.equals(contact.id)) {
                contactBox.getStyleClass().add("selected");
            }

            contactListContainer.getChildren().add(contactBox);
        }
    }

    private void deleteConversation(Contact contact) {
        allContacts.remove(contact);
        if (currentConversationId != null
                && currentConversationId.equals(contact.id)) {
            currentConversationId = null;
            root.setCenter(placeholderView);
        }
        renderContactList(allContacts);
    }

    /**
     * Helper to create the Circular Avatar component.
     */
    private StackPane createAvatarPane(String name, String colorHex, double size,
            double fontSize, boolean isOnline) {
        StackPane stack = new StackPane();
        stack.setPrefSize(size, size);
        stack.setAlignment(Pos.CENTER);

        Circle bg = new Circle(size / 2);
        bg.getStyleClass().add("avatar");
        bg.setStyle("-fx-fill: " + colorHex + ";");

        Label initials = new Label(getInitials(name));
        initials.getStyleClass().add("initials");
        initials.setStyle("-fx-font-size: " + fontSize
                + "px; -fx-text-fill: white; -fx-font-weight: bold;");

        stack.getChildren().addAll(bg, initials);

        if (isOnline) {
            Circle onlineDot = new Circle(6);
            onlineDot.getStyleClass().add("green-dot");
            StackPane.setAlignment(onlineDot, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(onlineDot, new Insets(0, 2, 2, 0));
            stack.getChildren().add(onlineDot);
        }

        return stack;
    }

    private VBox createPlaceholderPane() {
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("chat-pane");

        Label icon = new Label("💬");
        icon.setStyle("-fx-font-size: 64px; -fx-text-fill: -text-secondary; "
                + "-fx-opacity: 0.5;");

        Label text = new Label("Select a conversation to start chatting");
        text.setStyle("-fx-font-size: 18px; -fx-text-fill: -text-secondary; "
                + "-fx-font-weight: bold;");

        box.getChildren().addAll(icon, text);
        return box;
    }

    /**
     * Creates the main chat area (Messages + Input).
     */
    private VBox createChatPane() {
        VBox pane = new VBox();
        pane.getStyleClass().add("chat-pane");
        VBox.setVgrow(pane, Priority.ALWAYS);

        // Top Header
        HBox topBar = new HBox(15);
        topBar.getStyleClass().add("top-bar");
        topBar.setPrefHeight(70);
        topBar.setMinHeight(70);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 25, 0, 25));

        currentChatAvatarPane = createAvatarPane("?", "#3f3f46", 40, 14, false);

        VBox titleBox = new VBox(2);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        currentChatLabel = new Label("Select a conversation");
        currentChatLabel.getStyleClass().add("current-chat-label");

        currentChatSubtitle = new Label("");
        currentChatSubtitle.getStyleClass().add("contact-sub");
        currentChatSubtitle.setMinWidth(Control.USE_PREF_SIZE);

        titleBox.getChildren().addAll(currentChatLabel, currentChatSubtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        chatHeaderActions = new HBox(10);
        chatHeaderActions.setAlignment(Pos.CENTER_RIGHT);

        topBar.getChildren().addAll(currentChatAvatarPane, titleBox, spacer,
                chatHeaderActions);

        // Message Scroll Area
        chatArea = new VBox(12);
        chatArea.getStyleClass().add("chat-area");
        chatArea.setPadding(new Insets(20));

        chatScrollPane = new ScrollPane(chatArea);
        chatScrollPane.getStyleClass().add("scroll-pane-clean");
        chatScrollPane.setFitToWidth(true);
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);

        inputArea = createInputArea();
        inputArea.setVisible(true);
        inputArea.setManaged(true);

        pane.getChildren().addAll(topBar, chatScrollPane, inputArea);
        return pane;
    }

    private HBox createInputArea() {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));

        messageInput = new TextField();
        messageInput.setPromptText("Type a message...");
        messageInput.getStyleClass().add("message-input");
        messageInput.setPrefHeight(45);
        HBox.setHgrow(messageInput, Priority.ALWAYS);

        Button sendButton = new Button("Send");
        sendButton.getStyleClass().add("send-btn");
        sendButton.setPrefHeight(45);

        sendButton.setOnAction(e -> sendMessage());
        messageInput.setOnAction(e -> sendMessage());

        box.getChildren().addAll(messageInput, sendButton);
        return box;
    }

    // ==============================================================================================
    //                                   CHAT LOGIC
    // ==============================================================================================
    /**
     * Opens a specific conversation, updates the header, and clears unread
     * count.
     */
    private void openChat(Contact contact) {
        this.currentConversationId = contact.id;

        updateChatHeader(contact);

        contact.unreadCount = 0;
        renderContactList(allContacts);

        chatArea.getChildren().clear();

        // Load messages from server
        try {
            List<Message> messages = chatHandler.getMessages(contact.id);
            String myId = ClientState.getInstance().getCurrentUserId();

            for (Message m : messages) {
                boolean isMe = m.senderId.equals(myId);
                String senderName = "Unknown";
                if (isMe) {
                    senderName = "You";
                } else {
                    // Find sender name in participants
                    for (User u : contact.participants) {
                        if (u.id.equals(m.senderId)) {
                            senderName = u.displayName;
                            break;
                        }
                    }
                }
                addMessage(m.content, senderName, isMe);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        root.setCenter(chatView);

        Platform.runLater(() -> messageInput.requestFocus());
    }

    private void updateChatHeader(Contact contact) {
        currentChatLabel.setText(contact.name);

        StackPane newAvatar
                = createAvatarPane(contact.name, contact.colorHex, 40, 14, false);
        HBox topBar = (HBox) currentChatLabel.getParent().getParent();
        topBar.getChildren().set(0, newAvatar);

        chatHeaderActions.getChildren().clear();

        if (contact.isGroup) {
            String names = contact.getParticipantNames();
            currentChatSubtitle.setText(names.isEmpty() ? "You" : "You, " + names);

            Button infoBtn = new Button("⋮");
            infoBtn.getStyleClass().add("icon-btn");
            infoBtn.setTooltip(new Tooltip("Group Info"));
            infoBtn.setOnAction(e -> showGroupInfoDialog(contact));
            chatHeaderActions.getChildren().add(infoBtn);
        } else {
            currentChatSubtitle.setText(contact.isOnline ? "Online" : "Offline");
        }
    }

    private void sendMessage() {
        String text = messageInput.getText().trim();
        if (!text.isEmpty() && currentConversationId != null) {

            // Capture data for background thread
            final String msgContent = text;
            final String convId = currentConversationId;
            final String myId = ClientState.getInstance().getCurrentUserId();

            // Find current contact to check type
            Contact current = allContacts.stream()
                    .filter(c -> c.id.equals(convId))
                    .findFirst()
                    .orElse(null);

            if (current == null) {
                return;
            }

            final boolean isGroup = current.isGroup;
            String recId = null;
            if (!isGroup) {
                for (User u : current.participants) {
                    if (!u.id.equals(myId)) {
                        recId = u.id;
                        break;
                    }
                }
            }
            final String recipientId = recId;

            // Clear input immediately for responsiveness
            messageInput.clear();
            messageInput.requestFocus();

            // Run network operation in background
            new Thread(() -> {
                try {
                    if (isGroup) {
                        chatHandler.sendToGroup(convId, myId, msgContent);
                        chatHandler.getMessages(convId);
                    } else {
                        if (recipientId != null) {
                            chatHandler.sendDM(convId, myId, msgContent, recipientId);
                            chatHandler.getMessages(convId);
                            System.out.println("Message sent request sent.");
                        } else {
                            System.err.println("Cannot send DM: Recipient not found in contact.");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * Adds a message bubble to the chat area. Handles styling distinctions
     * between sent/received messages and sender name display.
     *
     * * @param text The message content
     * @param senderName Name of the sender (displayed if group chat + received
     * message)
     * @param isSentByMe Determines alignment and color (Right/Blue vs
     * Left/Gray)
     */
    private void addMessage(String text, String senderName, boolean isSentByMe) {
        System.out.println("Adding message: " + text + " | From: " + senderName
                + " | SentByMe: " + isSentByMe);
        HBox messageBox = new HBox();
        messageBox.setPadding(new Insets(2, 0, 2, 0));

        VBox bubbleContainer = new VBox(2);

        Contact currentChat = allContacts.stream()
                .filter(c -> c.id.equals(currentConversationId))
                .findFirst()
                .orElse(null);

        // Logic: Show sender name only in Groups, for received messages
        if (!isSentByMe && currentChat != null && currentChat.isGroup
                && senderName != null) {
            Label nameLabel = new Label(senderName);
            String nameColor = getNameColor(senderName);
            nameLabel.setStyle("-fx-text-fill: " + nameColor
                    + ("; -fx-font-size: 11px; -fx-font-weight: bold; "
                    + "-fx-padding: 0 5 0 5;"));
            bubbleContainer.getChildren().add(nameLabel);
        }

        Label messageLabel = new Label(text);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(500);

        if (isSentByMe) {
            messageLabel.getStyleClass().add("message-sent");
            bubbleContainer.setAlignment(Pos.CENTER_RIGHT);
            messageBox.setAlignment(Pos.CENTER_RIGHT);
        } else {
            messageLabel.getStyleClass().add("message-received");
            bubbleContainer.setAlignment(Pos.CENTER_LEFT);
            messageBox.setAlignment(Pos.CENTER_LEFT);
        }

        // Copy functionality context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
        });
        messageLabel.setContextMenu(contextMenu);

        bubbleContainer.getChildren().add(messageLabel);
        messageBox.getChildren().add(bubbleContainer);

        chatArea.getChildren().add(messageBox);
        chatArea.heightProperty().addListener(
                (o, old, n) -> chatScrollPane.setVvalue(1.0));
    }

    /**
     * Generates a consistent color based on the hash of the sender's name.
     * Ensures "Alice" always gets the same color, distinct from "Bob".
     */
    private String getNameColor(String name) {
        if (name == null) {
            return "#8b5cf6";
        }
        int hash = name.hashCode();
        String[] colors = {"#ef4444", "#3b82f6", "#10b981", "#f59e0b",
            "#8b5cf6", "#ec4899", "#06b6d4", "#f97316"};
        return colors[Math.abs(hash) % colors.length];
    }

    /**
     * Displays a logout confirmation dialog.
     */
    private void showLogoutDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Sign Out");
        dialog.setHeaderText(null);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(
                getClass().getResource("/styles.css").toExternalForm());

        if (root.getStyleClass().contains("light-theme")) {
            dialogPane.getStyleClass().add("light-theme");
        }

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label instruction = new Label("Are you sure you want to sign out?");
        instruction.getStyleClass().add("dialog-text");

        content.getChildren().add(instruction);
        dialogPane.setContent(content);

        ButtonType yesType = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
        ButtonType noType = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(yesType, noType);

        Button yesBtn = (Button) dialogPane.lookupButton(yesType);
        yesBtn.getStyleClass().add("logout-btn");

        Button noBtn = (Button) dialogPane.lookupButton(noType);
        noBtn.getStyleClass().add("simple-btn");

        dialog.setResultConverter(btn -> btn == yesType);

        Optional<Boolean> result = dialog.showAndWait();
        result.ifPresent(confirmed -> {
            if (confirmed) {
                Stage root = (Stage) this.root.getScene().getWindow();
                root.setScene(new HomeScene(root));
            }
        });
    }

    // Helper to get initials from a name (e.g., "Alice Johnson" -> "AJ")
    private String getInitials(String name) {
        if (name == null || name.isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

}
