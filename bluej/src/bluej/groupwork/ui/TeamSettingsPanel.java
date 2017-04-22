/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015,2016,2017  Michael Kolling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.groupwork.ui;

import java.util.Arrays;
import java.util.List;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import bluej.Config;
import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamSettingsController;
import bluej.groupwork.TeamSettingsController.ServerType;
import bluej.groupwork.TeamworkProvider;
import bluej.groupwork.actions.ValidateConnectionAction;
import bluej.utility.Debug;
import bluej.utility.javafx.HorizontalRadio;
import bluej.utility.javafx.JavaFXUtil;

/**
 * A panel for team settings.
 * 
 * @author fisker
 */
public class TeamSettingsPanel extends FlowPane
{
    private TeamSettingsController teamSettingsController;
    private TeamSettingsDialog teamSettingsDialog;

    private GridPane personalPane;
    private GridPane locationPane;

    private Label serverLabel    = new Label(Config.getString("team.settings.server"));
    private Label prefixLabel    = new Label(Config.getString("team.settings.prefix"));
    private Label protocolLabel  = new Label(Config.getString("team.settings.protocol"));
    private Label uriLabel       = new Label(Config.getString("team.settings.uri"));

    private Label yourNameLabel  = new Label(Config.getString("team.settings.yourName"));
    private Label yourEmailLabel = new Label(Config.getString("team.settings.yourEmail"));
    private Label userLabel      = new Label(Config.getString("team.settings.user"));
    private Label passwordLabel  = new Label(Config.getString("team.settings.password"));
    private Label groupLabel     = new Label(Config.getString("team.settings.group"));


    private final HorizontalRadio<ServerType> serverTypes;

    private final TextField serverField = new TextField();
    private final TextField prefixField = new TextField();
    private final ComboBox protocolComboBox = new ComboBox();
    private final TextField uriField = new TextField();

    private final TextField yourNameField = new TextField();
    private final TextField yourEmailField = new TextField();
    private final TextField userField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final TextField groupField = new TextField();

    private Button validateButton;
    private CheckBox useAsDefault;
    private ServerType selectedServerType = null;

    String[] personalLabels = {
            "team.settings.yourName",
            "team.settings.yourEmail",
            "team.settings.user",
            "team.settings.password",
            "team.settings.group"
    };

    String[] locationLabels = {
            "team.settings.prefix",
            "team.settings.uri",
            "team.settings.protocol"
    };

    public TeamSettingsPanel(TeamSettingsController teamSettingsController, TeamSettingsDialog dialog, ObservableList<String> styleClass)
    {
        this.teamSettingsController = teamSettingsController;
        this.teamSettingsDialog = dialog;

        JavaFXUtil.addStyleClass(this, styleClass);

        serverTypes = new HorizontalRadio(Arrays.asList(ServerType.Subversion, ServerType.Git));
        serverTypes.select(ServerType.Subversion);

        HBox langBox = new HBox();
        langBox.getChildren().add(new Label(Config.getString("team.settings.server")));
        langBox.getChildren().addAll(serverTypes.getButtons());
        langBox.setAlignment(Pos.BASELINE_CENTER);
        this.getChildren().add(langBox);

        useAsDefault = new CheckBox(Config.getString("team.settings.rememberSettings"));

        createPanes();
        preparePanes(serverTypes.selectedProperty().get());

        this.getChildren().addAll(new Label("Location"), locationPane,
                                  new Label("Personal"), personalPane
        );

        JavaFXUtil.addChangeListenerPlatform(serverTypes.selectedProperty(), type -> {
            preparePanes(type);
            updateOKButtonBinding();
        });

        getChildren().add(useAsDefault);
        ValidateConnectionAction validateConnectionAction = new ValidateConnectionAction(
                Config.getString("team.settings.checkConnection"), this, dialog::getOwner);
        validateButton = new Button(validateConnectionAction.getName());
        validateButton.setOnAction(event -> validateConnectionAction.actionPerformed(null));
        getChildren().add(validateButton);

        setupContent();
        updateOKButtonBinding();
        if (!teamSettingsController.hasProject()){
            useAsDefault.setSelected(true);
            useAsDefault.setDisable(true);
        }
    }

//    private GridPane addPane(String[] labels)
//    {
//        GridPane gridPane = new GridPane();
//        JavaFXUtil.addStyleClass(gridPane, "grid");
//
//        List<TextField> fields = new ArrayList<>();
//
//        for (int i = 0; i < labels.length; i++) {
//            Label label = new Label(Config.getString(labels[i]));
//            label.setPrefWidth(100);
//            gridPane.add(label, 0, i);
//
//            TextField field = new TextField();
//            fields.add(field);
//            JavaFXUtil.addChangeListener(field.textProperty(), text -> updateOKButton());
//            gridPane.add(field, 1, i);
//        }
//
//        return gridPane;
//    }

    private void createPanes()
    {
        locationPane = createGridPane(Config.getString("team.settings.location"));
        personalPane = createGridPane(Config.getString("team.settings.personal"));
    }

    private GridPane createGridPane(String title)
    {
        GridPane pane = new GridPane();
        pane.getStyleClass().add("grid");

        pane.setHgap(10);
        pane.setVgap(10);
        pane.setPadding(new Insets(20, 150, 10, 10));


        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPrefWidth(100);
        // Second column gets any extra width
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPrefWidth(260);
        column2.setHgrow(Priority.ALWAYS);
        pane.getColumnConstraints().addAll(column1, column2);

        return pane;
    }

    private void preparePanes(ServerType type)
    {
        prepareLocationPane(type);
        preparePersonalPane(type);

        setProviderSettings();

        switch (type) {
            case Subversion:
                useAsDefault.setDisable(false);
                break;
            case Git:
                // on git we always save.
                useAsDefault.setSelected(true);
                useAsDefault.setDisable(true);
                break;
            default:
                Debug.reportError(type + " is not recognisable as s server type");
        }
    }

    private void preparePersonalPane(ServerType type)
    {
        personalPane.getChildren().clear();
        personalPane.setGridLinesVisible(false);
        personalPane.setGridLinesVisible(true);

//        yourNameField.setPromptText(Config.getString("team.settings.yourName"));
        // Request focus on the username field by default.
        Platform.runLater(() -> yourNameField.requestFocus());

        switch (type) {
            case Subversion:
                personalPane.addRow(0, userLabel, userField);
                personalPane.addRow(1, passwordLabel, passwordField);
                personalPane.addRow(2, groupLabel, groupField);
                break;
            case Git:
                personalPane.addRow(0, yourNameLabel, yourNameField);
                personalPane.addRow(1, yourEmailLabel, yourEmailField);
                personalPane.addRow(2, userLabel, userField);
                personalPane.addRow(3, passwordLabel, passwordField);
                break;
            default:
                Debug.reportError(type + " is not recognisable as s server type");
        }
    }

    private void prepareLocationPane(ServerType type)
    {
        locationPane.getChildren().clear();
        protocolComboBox.setEditable(false);

        personalPane.setGridLinesVisible(false);
        locationPane.setGridLinesVisible(true);

        switch (type) {
            case Subversion:
                locationPane.addRow(0, serverLabel, serverField);
                locationPane.addRow(1, prefixLabel, prefixField);
                locationPane.addRow(2, protocolLabel, protocolComboBox);
                break;
            case Git:
                locationPane.addRow(0, uriLabel, uriField);
                break;
            default:
                Debug.reportError(type + " is not recognisable as s server type");
        }
    }
        
    private void setupContent()
    {
        String user = teamSettingsController.getPropString("bluej.teamsettings.user");
        if (user != null) {
            setUser(user);
        }
        
        String yourName = teamSettingsController.getPropString("bluej.teamsettings.yourName");
        if (yourName != null){
            setYourName(yourName);
        }
        
        String yourEmail = teamSettingsController.getPropString("bluej.teamsettings.yourEmail");
        if (yourEmail != null){
            setYourEmail(yourEmail);
        }
        
        String password = teamSettingsController.getPasswordString();
        if (password != null) {
            setPassword(password);
        }
        String group = teamSettingsController.getPropString("bluej.teamsettings.groupname");
        if(group != null) {
            setGroup(group);
        }
        String useAsDefault = teamSettingsController.getPropString("bluej.teamsettings.useAsDefault");
        if (useAsDefault != null) {
            setUseAsDefault(Boolean.getBoolean(useAsDefault));
        }
        
        String providerName = teamSettingsController.getPropString("bluej.teamsettings.vcs");
        // We always go through the providers.  If the user had no preference,
        // we select the first one, and update the email/name enabled states accordingly:
        List<TeamworkProvider> teamProviders = teamSettingsController.getTeamworkProviders();
        for (int index = 0; index < teamProviders.size(); index++) {
            TeamworkProvider provider = teamProviders.get(index);
            if (provider.getProviderName().equalsIgnoreCase(providerName)
                || (providerName == null && index == 0)) { // Select first if no stored preference
                serverTypes.select(ServerType.valueOf(teamProviders.get(index).getProviderName()));
                //checks if this provider needs your name and your e-mail.
                if (provider.needsEmail()){
                    if (teamSettingsController.getProject() != null){
                        //settings panel being open within a project.
                        //fill the data.
                        File respositoryRoot = teamSettingsController.getProject().getProjectDir();
                        yourEmailField.setText(provider.getYourEmailFromRepo(respositoryRoot));
                        yourEmailField.setDisable(true);
                        yourNameField.setText(provider.getYourNameFromRepo(respositoryRoot));
                        yourNameField.setDisable(true);
                        this.useAsDefault.setSelected(true); // on git we always save.
                    }
                }
                break;
            }
        }
        
        setProviderSettings();
    }
    
    /**
     * Set settings to provider-specific values (repository prefix, server, protocol).
     * The values are remembered on a per-provider basis; this sets the fields to show
     * the remembered values for the selected provider. 
     */
    private void setProviderSettings()
    {
        String keyBase = "bluej.teamsettings."
            + getSelectedProvider().getProviderName().toLowerCase() + ".";
        
        String prefix = teamSettingsController.getPropString(keyBase + "repositoryPrefix");
        if (prefix != null) {
            setPrefix(prefix);
        }
        String server = teamSettingsController.getPropString(keyBase + "server");
        if (server != null) {
            setServer(server);
        }

        fillProtocolSelections();
        
        String protocol = readProtocolString();
        if (protocol != null){
            setProtocol(protocol);
        }
    }

    /**
     * Empty the protocol selection box, then fill it with the available protocols
     * from the currently selected teamwork provider.
     */
    private void fillProtocolSelections()
    {
        ServerType type = serverTypes.selectedProperty().get();
        if (type != selectedServerType) {
            selectedServerType = type;
            protocolComboBox.getItems().clear();

            TeamworkProvider provider = teamSettingsController.getTeamworkProvider(type);
            protocolComboBox.getItems().addAll(Arrays.asList(provider.getProtocols()));
        }
    }
    
    private String readProtocolString()
    {
        String keyBase = "bluej.teamsettings."
            + getSelectedProvider().getProviderName().toLowerCase() + "."; 
        return teamSettingsController.getPropString(keyBase + "protocol");
    }

    private void setUser(String user)
    {
        userField.setText(user);
    }
    
    private void setYourName(String yourName)
    {
        yourNameField.setText(yourName);
    }
    
    private void setYourEmail(String yourEmail)
    {
        yourEmailField.setText(yourEmail);
    }
    
    private void setPassword(String password)
    {
        passwordField.setText(password);
    }
    
    private void setGroup(String group)
    {
        groupField.setText(group);
    }
    
    private void setPrefix(String prefix)
    {
        prefixField.setText(prefix);
    }
    
    private void setServer(String server)
    {
        serverField.setText(server);
    }
    
    /**
     * Set the protocol to that identified by the given protocol key.
     */
    private void setProtocol(String protocolKey)
    {
        String protocolLabel = getSelectedProvider().getProtocolLabel(protocolKey);
        protocolComboBox.getSelectionModel().select(protocolLabel);
    }
    
    private void setUseAsDefault(boolean use)
    {
        useAsDefault.setSelected(use);
    }
    
    public TeamworkProvider getSelectedProvider()
    {
        return teamSettingsController.getTeamworkProviders().stream()
                .filter(provider -> provider.getProviderName().equals(serverTypes.selectedProperty().get().name()))
                .findAny().get();
    }
    
    private String getUser()
    {
        return userField.getText();
    }

    private String getPassword()
    {
        return passwordField.getText();
    }

    private String getGroup()
    {
        //DCVS does not have group.
        if (getSelectedProvider().needsEmail()){
            return "";
        }
        return groupField.getText();
    }
    
    private String getPrefix()
    {
        if (getSelectedProvider().needsEmail()) {
            try {
                URI uri = new URI(uriField.getText());
                return uri.getPath();
            } catch (URISyntaxException ex) {
                return null;
            }
        }
        return prefixField.getText();
    }
    
    private String getServer()
    {
        if (getSelectedProvider().needsEmail()) {
            try {
                URI uri = new URI(uriField.getText());
                return uri.getHost();
            } catch (URISyntaxException ex) {
                return null;
            }
        }
        return serverField.getText();
    }
    
    private String getProtocolKey()
    {
        if (getSelectedProvider().needsEmail()) {
            try {
                URI uri = new URI(uriField.getText());
                return uri.getScheme();
            } catch (URISyntaxException ex) {
                return null;
            }
        }
        int protocol = protocolComboBox.getSelectionModel().getSelectedIndex();
        return getSelectedProvider().getProtocolKey(protocol);
    }
    
    public boolean getUseAsDefault()
    {
        return useAsDefault.isSelected();
    }
    
    private String getYourName(){
        return yourNameField.getText();
    }
    
    private String getYourEmail(){
        return yourEmailField.getText();
    }
    
    public TeamSettings getSettings() {
        TeamSettings result = new TeamSettings(getSelectedProvider(), getProtocolKey(),
                getServer(), getPrefix(), getGroup(), getUser(), getPassword());
        result.setYourEmail(getYourEmail());
        result.setYourName(getYourName());
        return result;
    }

    /**
     * Check whether the "ok" button should be enabled or disabled according
     * to whether required fields have been provided.
     */
    private void updateOKButtonBinding()
    {
        teamSettingsDialog.getOkButton().disableProperty().unbind();

        BooleanBinding enabled = userField.textProperty().isEmpty();
        switch (serverTypes.selectedProperty().get()) {
            case Subversion:
                enabled = enabled.or(serverField.textProperty().isEmpty());
                break;
            case Git:
                enabled = enabled.or(uriField.textProperty().isEmpty())
                        .or(yourNameField.textProperty().isEmpty())
                        .or(yourEmailField.textProperty().isEmpty())
                        .or(Bindings.createBooleanBinding(() -> !yourEmailField.getText().contains("@"), yourEmailField.textProperty()));
                break;
        }

        teamSettingsDialog.getOkButton().disableProperty().bind(enabled);
    }

    /**
     * Disable the fields used to specify the repository:
     * group, prefix, server and protocol
     */
    public void disableRepositorySettings()
    {
        groupField.setDisable(true);
        prefixField.setDisable(true);
        serverField.setDisable(true);
        protocolComboBox.setDisable(true);
        uriField.setDisable(true);

        if (uriField.isVisible() && uriField.getText().isEmpty()){
            //update uri.
            uriField.setText(TeamSettings.getURI(readProtocolString(), serverField.getText(), prefixField.getText()));
        }

        groupLabel.setDisable(true);
        prefixLabel.setDisable(true);
        serverLabel.setDisable(true);
        protocolLabel.setDisable(true);
    }


    class specialTextField
    {
        public TextField field;
        public Label label;
        public int special = 0;

        public specialTextField(String name)
        {
            label = new Label(name);
            field = new TextField();
            field.setPromptText(name);
        }

        public specialTextField(String name, int special)
        {
            this(name);
            this.special = special;
        }
    }
}
