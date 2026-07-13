package org;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import com.toedter.calendar.JDateChooser;
// iText imports for PDF functionality
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Main application class that builds the UI and manages application state.
 * CardCraft - A comprehensive ID card generation application with user management
 */
public class IDMateApp extends JFrame {
    // Encapsulation - private fields for internal state management
    private CardLayout cardLayout; // Manages different application screens
    private JPanel mainPanel; // Main container panel for all screens
    private Map<String, UserAccount> userAccounts = new HashMap<>(); // Stores user accounts with email as key
    private String currentUser; // Tracks currently logged in user's email
    private String userName, userEmail; // Stores current user's name and email
    private String templateType; // Stores selected ID card template type
    private Map<String, java.util.List<SavedCard>> userCards = new HashMap<>(); // Stores user's saved cards
    private JLabel cardPhotoLabel; // Label for displaying profile photo on card
    private JPanel frontCardPanel, backCardPanel; // Panels for card front and back preview
    private BufferedImage profileImage; // Stores user's profile image
    private Map<String, JComponent> inputFields = new HashMap<>(); // Maps field names to input components
    private Map<String, JLabel> frontCardLabels = new HashMap<>(); // Maps field names to front card labels
    private Map<String, JLabel> backCardLabels = new HashMap<>(); // Maps field names to back card labels
    private Map<String, LabelStyle> labelStyles = new HashMap<>(); // Stores styling for each label
    private BufferedImage generatedFrontImage, generatedBackImage; // Stores final generated card images
    private BufferedImage signatureImage; // Stores user's signature image
    private JLabel signatureLabel; // Label for displaying signature on card

    // Color constants from the provided palette - LIGHT THEME
    private final Color DARK_BG = new Color(48, 53, 46); // #30352e - Dark green-gray for panels
    private final Color PANEL_BG = new Color(69, 68, 66); // #454442 - Medium gray for panels
    private final Color ACCENT_GREEN = new Color(139, 154, 135); // #8B9A87 - Soft green accent
    private final Color ACCENT_RED = new Color(77, 29, 27); // #4D1D1B - Deep red accent
    private final Color ACCENT_BLUE = new Color(77, 90, 156); // #4D5A9C - Muted blue accent

    // Derived colors for the theme
    private final Color BG_LIGHT = new Color(245, 240, 235); // Light warm background
    private final Color FIELD_BG = new Color(230, 225, 220); // Light field background
    private final Color TEXT_DARK = new Color(48, 53, 46); // Dark text color
    private final Color TEXT_LIGHT = new Color(235, 230, 225); // Light text color
    private final Color BORDER_COLOR = new Color(139, 154, 135); // Green border
    private final Color BUTTON_COLOR = new Color(77, 90, 156); // Blue button
    private final Color BUTTON_HOVER = new Color(100, 115, 180); // Lighter blue hover
    private final Color NAV_BAR = new Color(69, 84, 66); // Dark nav bar
    private final Color CARD_BG = new Color(255, 252, 248); // Very light warm white for cards

    private JLabel previewFrontLabel, previewBackLabel; // Labels for previewing generated cards
    private JPanel savedCardsPanel; // Panel for displaying saved cards
    private JPanel idCardPageContainer; // Container for ID card design page
    private Point photoInitialClick; // Tracks initial click position for photo dragging
    private boolean photoIsResizing = false; // Flag for photo resize operation
    private JPanel selectionPanel; // Panel for template selection page
    private JLabel helloLabel; // Label for greeting user
    private JLabel selectedLabel = null; // Currently selected label for editing
    private JComboBox<String> labelFontCombo, labelSizeCombo; // Combo boxes for font and size selection
    private JButton labelColorBtn; // Button for changing text color

    // Static inner class to store label styling information
    static class LabelStyle {
        String fontName = "Playfair Display"; // Fancy professional font
        int fontSize = 16; // Default font size
        Color color = new Color(48, 53, 46); // Dark green-gray color
    }

    // Constructor - initializes the main application window and components
    public IDMateApp() {
        setTitle("CardCraft"); // Set window title
        setDefaultCloseOperation(EXIT_ON_CLOSE); // Close application on window close
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Start application in maximized mode

        // Load existing users from file when program starts
        loadUsersFromFile();

        cardLayout = new CardLayout(); // Create card layout for screen management
        mainPanel = new JPanel(cardLayout); // Create main panel with card layout

        // Add all application pages to the card layout
        mainPanel.add(createLoginPage(), "Login"); // Add login page
        mainPanel.add(createRegistrationPage(), "Register"); // Add registration page
        mainPanel.add(createSelectionPage(), "Selection"); // Add template selection page
        idCardPageContainer = new JPanel(new BorderLayout()); // Create container for ID card page
        mainPanel.add(idCardPageContainer, "IDCard"); // Add ID card design page
        mainPanel.add(createPreviewPage(), "Preview"); // Add preview page

        add(mainPanel); // Add main panel to frame
        cardLayout.show(mainPanel, "Login"); // Show login page initially
        setVisible(true); // Make frame visible
    }

    /**
     * Creates a rounded text field with custom styling
     */
    private JTextField createRoundedTextField(int columns) {
        JTextField field = new JTextField(columns) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int textX = 10;
                int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), textX, textY);
                g2.dispose();
            }
        };
        field.setOpaque(false);
        field.setBackground(FIELD_BG);
        field.setForeground(TEXT_DARK);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        field.setCaretColor(TEXT_DARK);
        field.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        return field;
    }

    /**
     * Creates a rounded password field with custom styling
     */
    private JPasswordField createRoundedPasswordField(int columns) {
        JPasswordField field = new JPasswordField(columns) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));
                g2.setColor(getForeground());
                g2.setFont(getFont());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        field.setOpaque(false);
        field.setBackground(FIELD_BG);
        field.setForeground(TEXT_DARK);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        field.setCaretColor(TEXT_DARK);
        field.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        return field;
    }

    /**
     * Loads all existing users from the registration file when program starts
     */
    private void loadUsersFromFile() {
        File userDataFile = new File("user_registration_data.txt");

        if (!userDataFile.exists()) {
            System.out.println("No existing user data file found. Starting fresh.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(userDataFile))) {
            String line;
            int loadedUsers = 0;

            while ((line = reader.readLine()) != null) {
                if (line.contains("|") && !line.contains("Timestamp") && !line.contains("---") && !line.contains("===")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 4) {
                        String name = parts[1].trim();
                        String email = parts[2].trim();
                        String password = parts[3].trim();

                        userAccounts.put(email, new UserAccount(name, email, password));
                        userCards.put(email, new ArrayList<>());
                        loadedUsers++;
                    }
                }
            }

            System.out.println("Loaded " + loadedUsers + " users from file.");

        } catch (IOException ex) {
            System.err.println("Error loading users from file: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Static nested class to represent user account data
    static class UserAccount {
        String name, email, password;
        UserAccount(String name, String email, String password) {
            this.name = name;
            this.email = email;
            this.password = password;
        }
    }

    // Static nested class to represent saved ID card data
    static class SavedCard {
        String type;
        BufferedImage frontImage, backImage;
        SavedCard(String type, BufferedImage front, BufferedImage back) {
            this.type = type;
            this.frontImage = front;
            this.backImage = back;
        }
    }

    // Inheritance - Custom dialog for image preview and cropping
    class ImagePreviewDialog extends JDialog {
        private BufferedImage originalImage;
        private BufferedImage croppedImage;
        private boolean cropped = false;
        private JLabel previewLabel;
        private JSlider sizeSlider;
        private int currentSize = 150;

        public ImagePreviewDialog(JFrame parent, BufferedImage image) {
            super(parent, "Adjust Image", true);
            this.originalImage = image;
            setLayout(new BorderLayout(10, 10));
            setSize(600, 500);
            setLocationRelativeTo(parent);

            JPanel previewPanel = new JPanel(new GridBagLayout());
            previewPanel.setBackground(BG_LIGHT);
            previewPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(ACCENT_GREEN), "Image Preview",
                    0, 0, new Font("Playfair Display", Font.BOLD, 14), TEXT_DARK));
            previewLabel = new JLabel();
            previewLabel.setBorder(BorderFactory.createLineBorder(ACCENT_GREEN, 2));
            updatePreviewImage();
            previewPanel.add(previewLabel);
            add(previewPanel, BorderLayout.CENTER);

            JPanel controlsPanel = new JPanel();
            controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
            controlsPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            controlsPanel.setBackground(BG_LIGHT);

            JLabel sizeLabel = new JLabel("Adjust Size:");
            sizeLabel.setFont(new Font("Playfair Display", Font.BOLD, 14));
            sizeLabel.setForeground(TEXT_DARK);
            sizeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            controlsPanel.add(sizeLabel);
            controlsPanel.add(Box.createVerticalStrut(10));

            sizeSlider = new JSlider(50, 400, 150);
            sizeSlider.setMajorTickSpacing(50);
            sizeSlider.setMinorTickSpacing(10);
            sizeSlider.setPaintTicks(true);
            sizeSlider.setPaintLabels(true);
            sizeSlider.setBackground(BG_LIGHT);
            sizeSlider.setForeground(TEXT_DARK);
            sizeSlider.addChangeListener(e -> {
                currentSize = sizeSlider.getValue();
                updatePreviewImage();
            });
            controlsPanel.add(sizeSlider);
            controlsPanel.add(Box.createVerticalStrut(20));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
            buttonPanel.setOpaque(false);
            JButton applyBtn = createRoundedButton("Apply", 120, 40);
            JButton cancelBtn = createRoundedButton("Cancel", 120, 40);

            applyBtn.addActionListener(e -> {
                croppedImage = new BufferedImage(currentSize, currentSize, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = croppedImage.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.drawImage(originalImage, 0, 0, currentSize, currentSize, null);
                g2.dispose();
                cropped = true;
                dispose();
            });

            cancelBtn.addActionListener(e -> {
                cropped = false;
                dispose();
            });

            buttonPanel.add(applyBtn);
            buttonPanel.add(cancelBtn);
            controlsPanel.add(buttonPanel);
            add(controlsPanel, BorderLayout.SOUTH);
            getContentPane().setBackground(BG_LIGHT);
        }

        private void updatePreviewImage() {
            BufferedImage preview = new BufferedImage(currentSize, currentSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = preview.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(originalImage, 0, 0, currentSize, currentSize, null);
            g2.dispose();
            previewLabel.setIcon(new ImageIcon(preview));
        }

        public boolean isCropped() { return cropped; }
        public BufferedImage getCroppedImage() { return croppedImage; }
    }

    /**
     * Creates the login page with email and password fields
     */
    private JPanel createLoginPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_LIGHT);

        // Create navigation bar
        JPanel navBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 50, 20));
        navBar.setBackground(NAV_BAR);
        navBar.setBorder(BorderFactory.createLineBorder(ACCENT_GREEN, 2));
        JLabel welcomeLabel = new JLabel("✦ Welcome to CardCraft ✦");
        welcomeLabel.setFont(new Font("Playfair Display", Font.BOLD, 48));
        welcomeLabel.setForeground(TEXT_LIGHT);
        navBar.add(welcomeLabel);
        panel.add(navBar, BorderLayout.NORTH);

        // Create center panel with login form
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(BG_LIGHT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 25, 15, 25);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Name field
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(new Font("Playfair Display", Font.BOLD, 26));
        nameLabel.setForeground(TEXT_DARK);
        centerPanel.add(nameLabel, gbc);
        JTextField nameField = createRoundedTextField(20);
        gbc.gridx = 1;
        centerPanel.add(nameField, gbc);

        // Email field
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Playfair Display", Font.BOLD, 26));
        emailLabel.setForeground(TEXT_DARK);
        centerPanel.add(emailLabel, gbc);
        JTextField emailField = createRoundedTextField(20);
        gbc.gridx = 1;
        centerPanel.add(emailField, gbc);

        // Password field with hint
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Playfair Display", Font.BOLD, 26));
        passLabel.setForeground(TEXT_DARK);
        centerPanel.add(passLabel, gbc);
        JPanel passFieldPanel = new JPanel();
        passFieldPanel.setLayout(new BoxLayout(passFieldPanel, BoxLayout.Y_AXIS));
        passFieldPanel.setOpaque(false);
        JPasswordField passField = createRoundedPasswordField(20);
        passFieldPanel.add(passField);
        passFieldPanel.add(Box.createVerticalStrut(5));
        JLabel passHintLabel = new JLabel("Password must be 8 characters or longer");
        passHintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        passHintLabel.setForeground(new Color(100, 95, 90));
        passFieldPanel.add(passHintLabel);
        gbc.gridx = 1;
        centerPanel.add(passFieldPanel, gbc);

        // Button panel
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);
        JButton loginBtn = createRoundedButton("Login", 180, 50);
        JButton registerBtn = createRoundedButton("Create New Account", 250, 50);
        buttonPanel.add(loginBtn);
        buttonPanel.add(registerBtn);
        centerPanel.add(buttonPanel, gbc);

        // Login action
        loginBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String password = new String(passField.getPassword());

            if (!validateName(name) || !validateEmail(email) || !validatePassword(password)) return;

            if (userAccounts.containsKey(email)) {
                UserAccount acc = userAccounts.get(email);
                if (acc.name.equals(name) && acc.email.equals(email) && acc.password.equals(password)) {
                    currentUser = email;
                    userName = acc.name;
                    userEmail = email;
                    updateSelectionPage();
                    cardLayout.show(mainPanel, "Selection");
                    nameField.setText("");
                    emailField.setText("");
                    passField.setText("");

                    JOptionPane.showMessageDialog(this,
                            "Login successful!\nWelcome back, " + acc.name + "!");
                } else {
                    StringBuilder errorMsg = new StringBuilder("Login failed!\n\n");
                    errorMsg.append("Please check the following:\n");
                    if (!acc.name.equals(name)) {
                        errorMsg.append("• Name does not match our records\n");
                    }
                    if (!acc.email.equals(email)) {
                        errorMsg.append("• Email does not match our records\n");
                    }
                    if (!acc.password.equals(password)) {
                        errorMsg.append("• Password is incorrect\n");
                    }
                    errorMsg.append("\nNote: All fields must match exactly (including capitalization).");
                    JOptionPane.showMessageDialog(this, errorMsg.toString(), "Login Failed", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                boolean userInFile = checkUserInFileExact(name, email, password);
                if (userInFile) {
                    JOptionPane.showMessageDialog(this,
                            "User found in records but not loaded properly.\n" +
                                    "Please register again or contact support.");
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Account not found! Please register.\n" +
                                    "No user found with the provided credentials.");
                }
            }
        });

        registerBtn.addActionListener(e -> cardLayout.show(mainPanel, "Register"));
        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates the user registration page
     */
    private JPanel createRegistrationPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_LIGHT);

        // Navigation bar
        JPanel navBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 50, 20));
        navBar.setBackground(NAV_BAR);
        JLabel titleLabel = new JLabel("✦ Create New Account ✦");
        titleLabel.setFont(new Font("Playfair Display", Font.BOLD, 42));
        titleLabel.setForeground(TEXT_LIGHT);
        navBar.add(titleLabel);
        panel.add(navBar, BorderLayout.NORTH);

        // Center panel with registration form
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(BG_LIGHT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 25, 15, 25);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Name field
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(new Font("Playfair Display", Font.BOLD, 26));
        nameLabel.setForeground(TEXT_DARK);
        centerPanel.add(nameLabel, gbc);
        JTextField regNameField = createRoundedTextField(20);
        gbc.gridx = 1;
        centerPanel.add(regNameField, gbc);

        // Email field
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Playfair Display", Font.BOLD, 26));
        emailLabel.setForeground(TEXT_DARK);
        centerPanel.add(emailLabel, gbc);
        JTextField regEmailField = createRoundedTextField(20);
        gbc.gridx = 1;
        centerPanel.add(regEmailField, gbc);

        // Password field with validation hint
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Playfair Display", Font.BOLD, 26));
        passLabel.setForeground(TEXT_DARK);
        centerPanel.add(passLabel, gbc);
        JPanel passFieldPanel = new JPanel();
        passFieldPanel.setLayout(new BoxLayout(passFieldPanel, BoxLayout.Y_AXIS));
        passFieldPanel.setOpaque(false);
        JPasswordField regPassField = createRoundedPasswordField(20);
        passFieldPanel.add(regPassField);
        passFieldPanel.add(Box.createVerticalStrut(5));
        JLabel passHintLabel = new JLabel("Password must be 8 characters or longer");
        passHintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        passHintLabel.setForeground(new Color(100, 95, 90));
        passFieldPanel.add(passHintLabel);
        gbc.gridx = 1;
        centerPanel.add(passFieldPanel, gbc);

        // Action buttons
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnPanel.setOpaque(false);
        JButton createBtn = createRoundedButton("Create Account", 220, 50);
        JButton backBtn = createRoundedButton("Back to Login", 200, 50);
        btnPanel.add(createBtn);
        btnPanel.add(backBtn);
        centerPanel.add(btnPanel, gbc);

        createBtn.addActionListener(e -> {
            String name = regNameField.getText().trim();
            String email = regEmailField.getText().trim();
            String password = new String(regPassField.getPassword());

            if (!validateName(name) || !validateEmail(email) || !validatePassword(password)) return;

            if (userAccounts.containsKey(email)) {
                JOptionPane.showMessageDialog(this, "Account already exists! Please login.");
            } else {
                UserAccount newAccount = new UserAccount(name, email, password);
                userAccounts.put(email, newAccount);
                userCards.put(email, new ArrayList<>());
                currentUser = email;
                userName = name;
                userEmail = email;

                boolean savedToFile = saveUserDataToFile(name, email, password);

                if (savedToFile) {
                    JOptionPane.showMessageDialog(this,
                            "Account created successfully.");
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Account created but failed to save data to file!");
                }

                updateSelectionPage();
                cardLayout.show(mainPanel, "Selection");
                regNameField.setText("");
                regEmailField.setText("");
                regPassField.setText("");
            }
        });

        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "Login"));
        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Saves user registration data to a text file
     */
    private boolean saveUserDataToFile(String name, String email, String password) {
        File userDataFile = new File("user_registration_data.txt");

        try {
            if (!userDataFile.exists()) {
                userDataFile.createNewFile();
                try (PrintWriter headerWriter = new PrintWriter(new FileWriter(userDataFile, true))) {
                    headerWriter.println("==========================================");
                    headerWriter.println("CardCraft User Registration Data");
                    headerWriter.println("==========================================");
                    headerWriter.println("Timestamp | Name | Email | Password");
                    headerWriter.println("------------------------------------------");
                }
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(userDataFile, true))) {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                writer.printf("%s | %s | %s | %s%n", timestamp, name, email, password);
                return true;
            }

        } catch (IOException ex) {
            System.err.println("Error saving user data to file: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Checks if user exists in the registration file with EXACT matching credentials
     */
    private boolean checkUserInFileExact(String name, String email, String password) {
        File userDataFile = new File("user_registration_data.txt");

        if (!userDataFile.exists()) {
            return false;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(userDataFile))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("|") && !line.contains("Timestamp") && !line.contains("---") && !line.contains("===")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 4) {
                        String fileName = parts[1].trim();
                        String fileEmail = parts[2].trim();
                        String filePassword = parts[3].trim();

                        if (fileName.equals(name) && fileEmail.equals(email) && filePassword.equals(password)) {
                            userAccounts.put(email, new UserAccount(name, email, password));
                            userCards.put(email, new ArrayList<>());
                            return true;
                        }
                    }
                }
            }
        } catch (IOException ex) {
            System.err.println("Error reading user file: " + ex.getMessage());
        }

        return false;
    }

    /**
     * Validates user name input
     */
    private boolean validateName(String name) {
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name cannot be empty!");
            return false;
        }
        return true;
    }

    /**
     * Validates email format
     */
    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Email cannot be empty!");
            return false;
        }
        if (!email.contains("@gmail.com") || email.indexOf("@") == 0) {
            JOptionPane.showMessageDialog(this, "Enter a valid email.");
            return false;
        }
        return true;
    }

    /**
     * Validates password length requirements
     */
    private boolean validatePassword(String password) {
        if (password.length() < 8) {
            JOptionPane.showMessageDialog(this, "Password must be 8 characters or longer!");
            return false;
        }
        return true;
    }

    /**
     * Creates the template selection page where users choose ID card type
     */
    private JPanel createSelectionPage() {
        selectionPanel = new JPanel(new BorderLayout());
        selectionPanel.setBackground(BG_LIGHT);

        // Navigation bar with user greeting
        JPanel navBar = new JPanel(new BorderLayout());
        navBar.setBackground(NAV_BAR);
        navBar.setBorder(BorderFactory.createLineBorder(ACCENT_GREEN, 2));
        navBar.setPreferredSize(new Dimension(0, 80));
        helloLabel = new JLabel("✦ Hello, User! ✦");
        helloLabel.setFont(new Font("Playfair Display", Font.BOLD, 32));
        helloLabel.setForeground(TEXT_LIGHT);
        helloLabel.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));
        JButton homeBtn = createRoundedButton("⌂ Back to Homepage", 220, 45);
        homeBtn.addActionListener(e -> cardLayout.show(mainPanel, "Login"));

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 15));
        rightPanel.setOpaque(false);
        rightPanel.add(homeBtn);

        navBar.add(helloLabel, BorderLayout.WEST);
        navBar.add(rightPanel, BorderLayout.EAST);
        selectionPanel.add(navBar, BorderLayout.NORTH);

        // Main content area with template selection and saved cards
        JPanel contentPanel = new JPanel(new BorderLayout(20, 0));
        contentPanel.setBackground(BG_LIGHT);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Left panel - template selection buttons
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBackground(BG_LIGHT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0;
        JLabel header = new JLabel("♢ Choose your ID Card:", SwingConstants.CENTER);
        header.setFont(new Font("Playfair Display", Font.BOLD, 36));
        header.setForeground(TEXT_DARK);
        gbc.gridy = 0;
        leftPanel.add(header, gbc);

        // Template selection buttons
        gbc.gridy = 1;
        JButton studentBtn = createSelectionButton("Student ID Card");
        leftPanel.add(studentBtn, gbc);
        gbc.gridy = 2;
        JButton businessBtn = createSelectionButton("Business ID Card");
        leftPanel.add(businessBtn, gbc);
        gbc.gridy = 3;
        JButton hospitalBtn = createSelectionButton("Hospital ID Card");
        leftPanel.add(hospitalBtn, gbc);

        // Button action listeners
        studentBtn.addActionListener(e -> {
            templateType = "Student";
            rebuildIDCardPage();
            cardLayout.show(mainPanel, "IDCard");
        });
        businessBtn.addActionListener(e -> {
            templateType = "Business";
            rebuildIDCardPage();
            cardLayout.show(mainPanel, "IDCard");
        });
        hospitalBtn.addActionListener(e -> {
            templateType = "Hospital";
            rebuildIDCardPage();
            cardLayout.show(mainPanel, "IDCard");
        });

        // Right panel - saved cards display
        JPanel rightSidePanel = new JPanel(new BorderLayout());
        rightSidePanel.setBackground(PANEL_BG);
        rightSidePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT_GREEN, 2),
                "★ Your Saved Cards", 0, 0, new Font("Playfair Display", Font.BOLD, 20), TEXT_LIGHT));
        rightSidePanel.setPreferredSize(new Dimension(400, 0));
        savedCardsPanel = new JPanel();
        savedCardsPanel.setLayout(new BoxLayout(savedCardsPanel, BoxLayout.Y_AXIS));
        savedCardsPanel.setBackground(PANEL_BG);
        JScrollPane scrollPane = new JScrollPane(savedCardsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        rightSidePanel.add(scrollPane, BorderLayout.CENTER);

        contentPanel.add(leftPanel, BorderLayout.CENTER);
        contentPanel.add(rightSidePanel, BorderLayout.EAST);
        selectionPanel.add(contentPanel, BorderLayout.CENTER);
        return selectionPanel;
    }

    /**
     * Updates the selection page with current user data and saved cards
     */
    private void updateSelectionPage() {
        if (helloLabel != null && userName != null) {
            helloLabel.setText("✦ Hello, " + userName + "! ✦");
        }
        if (savedCardsPanel == null) return;
        savedCardsPanel.removeAll();
        if (currentUser != null && userCards.containsKey(currentUser)) {
            java.util.List<SavedCard> cards = userCards.get(currentUser);
            for (SavedCard card : cards) {
                JPanel cardItem = new JPanel(new BorderLayout(5, 5));
                cardItem.setMaximumSize(new Dimension(380, 120));
                cardItem.setBackground(new Color(80, 78, 76));
                cardItem.setBorder(BorderFactory.createLineBorder(ACCENT_GREEN, 2));
                cardItem.setCursor(new Cursor(Cursor.HAND_CURSOR));
                JLabel thumbnail = new JLabel();
                if (card.frontImage != null) {
                    ImageIcon icon = new ImageIcon(card.frontImage.getScaledInstance(100, 80, java.awt.Image.SCALE_SMOOTH));
                    thumbnail.setIcon(icon);
                }
                JLabel typeLabel = new JLabel("◆ " + card.type + " ID Card");
                typeLabel.setFont(new Font("Playfair Display", Font.BOLD, 16));
                typeLabel.setForeground(TEXT_LIGHT);
                cardItem.add(thumbnail, BorderLayout.WEST);
                cardItem.add(typeLabel, BorderLayout.CENTER);
                cardItem.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) { viewSavedCard(card); }
                });
                savedCardsPanel.add(cardItem);
                savedCardsPanel.add(Box.createVerticalStrut(10));
            }
        }
        savedCardsPanel.revalidate();
        savedCardsPanel.repaint();
    }

    /**
     * Displays a saved card in the preview page
     */
    private void viewSavedCard(SavedCard card) {
        generatedFrontImage = card.frontImage;
        generatedBackImage = card.backImage;
        updatePreviewImages();
        cardLayout.show(mainPanel, "Preview");
    }

    /**
     * Returns mandatory fields based on template type
     */
    private java.util.List<String> getMandatoryFields() {
        java.util.List<String> mandatory = new ArrayList<>();
        if ("Student".equals(templateType)) {
            mandatory.addAll(Arrays.asList("name", "class", "section", "organization", "issueDate", "expiryDate", "address"));
        } else if ("Business".equals(templateType)) {
            mandatory.addAll(Arrays.asList("name", "email", "contact", "employeeId", "designation", "organization", "issueDate", "expiryDate", "address"));
        } else if ("Hospital".equals(templateType)) {
            mandatory.addAll(Arrays.asList("name", "email", "contact", "specialization", "organization", "issueDate", "expiryDate", "address"));
        }
        return mandatory;
    }

    /**
     * Validates that all mandatory fields are filled
     */
    private boolean validateMandatoryFields() {
        java.util.List<String> mandatory = getMandatoryFields();
        for (String field : mandatory) {
            JComponent comp = inputFields.get(field);
            if (comp == null) continue;
            String value = "";
            if (comp instanceof JTextField) { value = ((JTextField) comp).getText().trim(); }
            else if (comp instanceof JDateChooser) {
                Date date = ((JDateChooser) comp).getDate();
                if (date != null) value = "filled";
            }
            if (value.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all mandatory fields.");
                return false;
            }
        }
        return true;
    }

    /**
     * Rebuilds the ID card design page based on selected template
     */
    private void rebuildIDCardPage() {
        profileImage = null;
        signatureImage = null;
        inputFields.clear();
        frontCardLabels.clear();
        backCardLabels.clear();
        labelStyles.clear();
        selectedLabel = null;
        photoInitialClick = null;
        photoIsResizing = false;
        idCardPageContainer.removeAll();

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_LIGHT);

        // Navigation bar for ID card design page
        JPanel navBar = new JPanel(new BorderLayout());
        navBar.setBackground(NAV_BAR);
        navBar.setBorder(BorderFactory.createLineBorder(ACCENT_GREEN, 2));
        navBar.setPreferredSize(new Dimension(0, 70));
        JButton backBtn = createRoundedButton("← Back", 150, 40);
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "Selection"));
        JLabel templateTitleLabel = new JLabel("✦ " + templateType + " ID Card Form", SwingConstants.CENTER);
        templateTitleLabel.setFont(new Font("Playfair Display", Font.BOLD, 28));
        templateTitleLabel.setForeground(TEXT_LIGHT);
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        leftPanel.setOpaque(false);
        leftPanel.add(backBtn);
        navBar.add(leftPanel, BorderLayout.WEST);
        navBar.add(templateTitleLabel, BorderLayout.CENTER);
        panel.add(navBar, BorderLayout.NORTH);

        // Scrollable design area
        JScrollPane scrollPane = new JScrollPane(createCardDesignPanel());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Generate card button
        JButton generateBtn = createRoundedButton("Generate Your Card ★", 300, 55);
        generateBtn.setFont(new Font("Playfair Display", Font.BOLD, 24));
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(BG_LIGHT);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        btnPanel.add(generateBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        generateBtn.addActionListener(e -> {
            if (!validateMandatoryFields()) return;
            captureCardImages();
            updatePreviewImages();
            if (currentUser != null && userCards.containsKey(currentUser)) {
                SavedCard newCard = new SavedCard(templateType, generatedFrontImage, generatedBackImage);
                userCards.get(currentUser).add(newCard);
                updateSelectionPage();
            }
            cardLayout.show(mainPanel, "Preview");
        });

        idCardPageContainer.add(panel, BorderLayout.CENTER);
        idCardPageContainer.revalidate();
        idCardPageContainer.repaint();
    }

    /**
     * Creates the main card design panel with input fields and preview
     */
    private JPanel createCardDesignPanel() {
        JPanel designPanel = new JPanel(new BorderLayout(20, 20));
        designPanel.setBackground(BG_LIGHT);
        designPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(BG_LIGHT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 10, 0, 10);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weighty = 1;
        gbc.gridx = 0; gbc.gridy = 0;
        JPanel inputPanel = createInputPanel();
        centerPanel.add(inputPanel, gbc);
        gbc.gridx = 1;
        JPanel previewPanel = createCardPreviewPanel();
        centerPanel.add(previewPanel, gbc);
        designPanel.add(centerPanel, BorderLayout.CENTER);
        return designPanel;
    }

    /**
     * Creates the input panel with form fields and customization options
     */
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(null);
        panel.setPreferredSize(new Dimension(500, 1020));
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createLineBorder(ACCENT_GREEN, 2, true));
        int y = 20;

        // Add template-specific fields
        if ("Student".equals(templateType)) {
            y = addField(panel, "Name:*", "name", y, true, false);
            y = addField(panel, "Class:*", "class", y, false, false);
            y = addField(panel, "Section:*", "section", y, false, false);
            y = addField(panel, "Department (Optional):", "department", y, false, false);
            y = addField(panel, "Organization:*", "organization", y, false, false);
            y += 10;
            y = addField(panel, "Issue Date: *", "issueDate", y, false, false);
            y = addField(panel, "Expiry Date: *", "expiryDate", y, false, false);
            y = addField(panel, "Contact:", "contact", y, false, false);
            y = addField(panel, "Address: *", "address", y, false, false);
        } else if ("Business".equals(templateType)) {
            y = addField(panel, "Name: *", "name", y, true, false);
            y = addField(panel, "Email: *", "email", y, false, false);
            y = addField(panel, "Contact: *", "contact", y, false, false);
            y = addField(panel, "Employee ID: *", "employeeId", y, false, false);
            y = addField(panel, "Designation: *", "designation", y, false, true);
            y = addField(panel, "Organization: *", "organization", y, false, true);
            y += 10;
            y = addField(panel, "Issue Date: *", "issueDate", y, false, false);
            y = addField(panel, "Expiry Date: *", "expiryDate", y, false, false);
            y = addField(panel, "Address: *", "address", y, false, false);
            y = addField(panel, "Terms & Conditions:", "termsConditions", y, false, false);
        } else if ("Hospital".equals(templateType)) {
            y = addField(panel, "Name: *", "name", y, true, false);
            y = addField(panel, "Email: *", "email", y, false, false);
            y = addField(panel, "Contact: *", "contact", y, false, false);
            y = addField(panel, "Specialization: *", "specialization", y, false, false);
            y = addField(panel, "Organization: *", "organization", y, false, true);
            y += 10;
            y = addField(panel, "Issue Date: *", "issueDate", y, false, false);
            y = addField(panel, "Expiry Date: *", "expiryDate", y, false, false);
            y = addField(panel, "Address: *", "address", y, false, false);
        }
        y += 20;

        // Photo upload button
        JButton uploadBtn = createRoundedButton("Import Photo", 200, 35);
        uploadBtn.setBounds(30, y, 200, 35);
        panel.add(uploadBtn);

        // Template color customization button
        JButton templateColorBtn = createRoundedButton("Template Color", 200, 35);
        templateColorBtn.setBounds(250, y, 200, 35);
        panel.add(templateColorBtn);
        final IDMateApp self = this;

        uploadBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                public boolean accept(File f) {
                    String name = f.getName().toLowerCase();
                    return f.isDirectory() || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                            name.endsWith(".png") || name.endsWith(".gif");
                }
                public String getDescription() { return "Image Files (*.jpg, *.jpeg, *.png, *.gif)"; }
            });
            if (chooser.showOpenDialog(self) == JFileChooser.APPROVE_OPTION) {
                try {
                    BufferedImage originalImage = ImageIO.read(chooser.getSelectedFile());
                    ImagePreviewDialog previewDialog = new ImagePreviewDialog(self, originalImage);
                    previewDialog.setVisible(true);
                    if (previewDialog.isCropped()) {
                        profileImage = previewDialog.getCroppedImage();
                        updateCardPreview();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(self, "Error loading image!");
                }
            }
        });

        templateColorBtn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(self, "Choose Template Color",
                    frontCardPanel != null ? frontCardPanel.getBackground() : Color.WHITE);
            if (c != null) {
                if (frontCardPanel != null) frontCardPanel.setBackground(c);
                if (backCardPanel != null) backCardPanel.setBackground(c);
            }
        });

        // Signature area
        y += 50;
        JLabel sigLabel = new JLabel("Signature Area:");
        sigLabel.setFont(new Font("Playfair Display", Font.BOLD, 14));
        sigLabel.setForeground(TEXT_LIGHT);
        sigLabel.setBounds(30, y, 150, 25);
        panel.add(sigLabel);
        y += 30;
        SignaturePanel sigPanel = new SignaturePanel();
        sigPanel.setBounds(30, y, 440, 120);
        sigPanel.setBackground(Color.WHITE);
        sigPanel.setBorder(BorderFactory.createLineBorder(ACCENT_GREEN, 2));
        panel.add(sigPanel);
        y += 130;

        // Signature control buttons
        JButton clearSigBtn = createRoundedButton("Clear Signature", 200, 30);
        clearSigBtn.setBounds(30, y, 200, 30);
        clearSigBtn.addActionListener(e -> {
            sigPanel.clear();
            signatureImage = null;
            updateCardPreview();
        });
        panel.add(clearSigBtn);

        JButton applySigBtn = createRoundedButton("Apply Signature", 200, 30);
        applySigBtn.setBounds(250, y, 200, 30);
        applySigBtn.addActionListener(e -> {
            signatureImage = sigPanel.getSignatureImage();
            updateCardPreview();
            JOptionPane.showMessageDialog(self, "Signature applied to card!");
        });
        panel.add(applySigBtn);

        // Text editing controls
        y += 40;
        JLabel editLabel = new JLabel("Edit Selected Text:");
        editLabel.setFont(new Font("Playfair Display", Font.BOLD, 16));
        editLabel.setForeground(TEXT_LIGHT);
        editLabel.setBounds(30, y, 440, 25);
        panel.add(editLabel);
        y += 35;

        // Font selection
        JLabel fontLabel = new JLabel("Font:");
        fontLabel.setFont(new Font("Playfair Display", Font.BOLD, 14));
        fontLabel.setForeground(TEXT_LIGHT);
        fontLabel.setBounds(30, y, 50, 25);
        panel.add(fontLabel);
        labelFontCombo = new JComboBox<>(new String[]{
                "Playfair Display", "Segoe UI", "Arial", "Verdana", "Tahoma", "Georgia", "Calibri", "Century Gothic"
        });
        labelFontCombo.setBounds(85, y, 150, 25);
        labelFontCombo.setEnabled(false);
        labelFontCombo.setBackground(FIELD_BG);
        labelFontCombo.setForeground(TEXT_DARK);
        panel.add(labelFontCombo);

        // Font size selection
        JLabel sizeLabel = new JLabel("Size:");
        sizeLabel.setFont(new Font("Playfair Display", Font.BOLD, 14));
        sizeLabel.setForeground(TEXT_LIGHT);
        sizeLabel.setBounds(250, y, 50, 25);
        panel.add(sizeLabel);
        labelSizeCombo = new JComboBox<>(new String[]{
                "12", "14", "16", "18", "20", "22", "24", "26", "28", "30", "32", "34", "36", "38", "40"
        });
        labelSizeCombo.setSelectedItem("16");
        labelSizeCombo.setBounds(305, y, 70, 25);
        labelSizeCombo.setEnabled(false);
        labelSizeCombo.setBackground(FIELD_BG);
        labelSizeCombo.setForeground(TEXT_DARK);
        panel.add(labelSizeCombo);

        // Text color button
        y += 40;
        labelColorBtn = createRoundedButton("◈ Text Color", 180, 35);
        labelColorBtn.setBounds(160, y, 180, 35);
        labelColorBtn.setEnabled(false);
        panel.add(labelColorBtn);

        labelFontCombo.addActionListener(e -> {
            if (selectedLabel != null) {
                String key = findLabelKey(selectedLabel);
                if (key != null) {
                    LabelStyle style = labelStyles.get(key);
                    style.fontName = (String) labelFontCombo.getSelectedItem();
                    updateCardPreview();
                }
            }
        });

        labelSizeCombo.addActionListener(e -> {
            if (selectedLabel != null) {
                String key = findLabelKey(selectedLabel);
                if (key != null) {
                    LabelStyle style = labelStyles.get(key);
                    style.fontSize = Integer.parseInt((String) labelSizeCombo.getSelectedItem());
                    updateCardPreview();
                }
            }
        });

        labelColorBtn.addActionListener(e -> {
            if (selectedLabel != null) {
                String key = findLabelKey(selectedLabel);
                if (key != null) {
                    LabelStyle style = labelStyles.get(key);
                    Color c = JColorChooser.showDialog(self, "Choose Text Color", style.color);
                    if (c != null) {
                        style.color = c;
                        updateCardPreview();
                    }
                }
            }
        });
        return panel;
    }

    /**
     * Finds the key associated with a label in the label maps
     */
    private String findLabelKey(JLabel label) {
        for (Map.Entry<String, JLabel> entry : frontCardLabels.entrySet()) {
            if (entry.getValue() == label) return entry.getKey();
        }
        for (Map.Entry<String, JLabel> entry : backCardLabels.entrySet()) {
            if (entry.getValue() == label) return entry.getKey();
        }
        return null;
    }

    /**
     * Adds a field to the input panel with proper formatting and validation
     */
    private int addField(JPanel panel, String labelText, String fieldName, int y, boolean setUserName, boolean noPrefix) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Playfair Display", Font.BOLD, 14));
        label.setForeground(TEXT_LIGHT);
        label.setBounds(30, y, 150, 25);
        panel.add(label);
        JComponent field;

        if (fieldName.contains("Date")) {
            JDateChooser dateChooser = new JDateChooser();
            dateChooser.setDateFormatString("dd/MM/yyyy");
            dateChooser.setBounds(190, y, 280, 30);
            dateChooser.getJCalendar().setBackground(FIELD_BG);
            field = dateChooser;
            dateChooser.addPropertyChangeListener("date", evt -> updateCardPreview());
        } else {
            JTextField textField = new JTextField();
            textField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            textField.setBounds(190, y, 280, 30);
            textField.setBackground(FIELD_BG);
            textField.setForeground(TEXT_DARK);
            textField.setCaretColor(TEXT_DARK);
            textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT_GREEN, 1, true),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
            if (setUserName && userName != null) { textField.setText(userName); }
            if (fieldName.equals("name")) {
                textField.addKeyListener(new KeyAdapter() {
                    public void keyTyped(KeyEvent e) {
                        char c = e.getKeyChar();
                        if (!Character.isLetter(c) && c != ' ' && c != KeyEvent.VK_BACK_SPACE) {
                            e.consume();
                        }
                    }
                });
            } else if (fieldName.equals("contact")) {
                textField.addKeyListener(new KeyAdapter() {
                    public void keyTyped(KeyEvent e) {
                        if (textField.getText().length() >= 11 && e.getKeyChar() != KeyEvent.VK_BACK_SPACE) { e.consume(); }
                        if (!Character.isDigit(e.getKeyChar()) && e.getKeyChar() != KeyEvent.VK_BACK_SPACE) { e.consume(); }
                    }
                });
            } else if (fieldName.equals("email")) {
                textField.addFocusListener(new FocusAdapter() {
                    public void focusLost(FocusEvent e) {
                        String email = textField.getText();
                        if (!email.isEmpty() && (!email.contains("@gmail.com") || email.indexOf("@") == 0)) {
                            JOptionPane.showMessageDialog(panel, "Enter a valid email.");
                        }
                    }
                });
            }
            textField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCardPreview(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { updateCardPreview(); }
                public void insertUpdate(javax.swing.event.DocumentEvent e) { updateCardPreview(); }
            });
            field = textField;
        }
        panel.add(field);
        inputFields.put(fieldName, field);
        return y + 45;
    }

    /**
     * Creates the card preview panel showing front and back of ID card
     */
    private JPanel createCardPreviewPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_LIGHT);

        // Front side preview
        JLabel frontLabel = new JLabel("♢ Front Side ♢");
        frontLabel.setFont(new Font("Playfair Display", Font.BOLD, 18));
        frontLabel.setForeground(TEXT_DARK);
        frontLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(frontLabel);
        panel.add(Box.createVerticalStrut(10));
        frontCardPanel = new JPanel(null);
        frontCardPanel.setPreferredSize(new Dimension(550, 350));
        frontCardPanel.setMaximumSize(new Dimension(550, 350));
        frontCardPanel.setBackground(CARD_BG);
        frontCardPanel.setBorder(BorderFactory.createLineBorder(ACCENT_GREEN, 2));
        setupFrontCard();
        panel.add(frontCardPanel);
        panel.add(Box.createVerticalStrut(20));

        // Back side preview
        JLabel backLabel = new JLabel("♢ Back Side ♢");
        backLabel.setFont(new Font("Playfair Display", Font.BOLD, 18));
        backLabel.setForeground(TEXT_DARK);
        backLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(backLabel);
        panel.add(Box.createVerticalStrut(10));
        backCardPanel = new JPanel(null);
        backCardPanel.setPreferredSize(new Dimension(550, 350));
        backCardPanel.setMaximumSize(new Dimension(550, 350));
        backCardPanel.setBackground(CARD_BG);
        backCardPanel.setBorder(BorderFactory.createLineBorder(ACCENT_GREEN, 2));
        setupBackCard();
        panel.add(backCardPanel);
        return panel;
    }

    /**
     * Sets up the front card panel with photo and labels
     */
    private void setupFrontCard() {
        cardPhotoLabel = new JLabel("Photo", SwingConstants.CENTER);
        cardPhotoLabel.setBounds(200, 30, 150, 150);
        cardPhotoLabel.setOpaque(true);
        cardPhotoLabel.setBackground(new Color(220, 215, 210));
        cardPhotoLabel.setBorder(BorderFactory.createLineBorder(ACCENT_GREEN, 2));
        cardPhotoLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPopupMenu photoPopup = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete Photo");
        deleteItem.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "You want to remove the pic?",
                    "Remove Photo",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                profileImage = null;
                cardPhotoLabel.setIcon(null);
                cardPhotoLabel.setText("Photo");
                updateCardPreview();
                JOptionPane.showMessageDialog(this, "Photo removed successfully!");
            }
        });
        photoPopup.add(deleteItem);

        cardPhotoLabel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    if (profileImage != null) { photoPopup.show(cardPhotoLabel, e.getX(), e.getY()); }
                } else {
                    photoInitialClick = e.getPoint();
                    int cornerSize = 20;
                    if (e.getX() > cardPhotoLabel.getWidth() - cornerSize &&
                            e.getY() > cardPhotoLabel.getHeight() - cornerSize) {
                        photoIsResizing = true;
                        cardPhotoLabel.setCursor(new Cursor(Cursor.SE_RESIZE_CURSOR));
                    } else {
                        photoIsResizing = false;
                        cardPhotoLabel.setCursor(new Cursor(Cursor.MOVE_CURSOR));
                    }
                }
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    if (profileImage != null) { photoPopup.show(cardPhotoLabel, e.getX(), e.getY()); }
                } else {
                    cardPhotoLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    photoIsResizing = false;
                }
            }
        });

        cardPhotoLabel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (photoInitialClick == null) return;
                if (photoIsResizing) {
                    int newWidth = Math.max(50, e.getX());
                    int newHeight = Math.max(50, e.getY());
                    cardPhotoLabel.setSize(newWidth, newHeight);
                    if (profileImage != null) {
                        ImageIcon icon = new ImageIcon(getScaledImage(profileImage, newWidth, newHeight));
                        cardPhotoLabel.setIcon(icon);
                    }
                } else {
                    int dx = e.getX() - photoInitialClick.x;
                    int dy = e.getY() - photoInitialClick.y;
                    Point loc = cardPhotoLabel.getLocation();
                    cardPhotoLabel.setLocation(loc.x + dx, loc.y + dy);
                }
            }
            public void mouseMoved(MouseEvent e) {
                int cornerSize = 20;
                if (e.getX() > cardPhotoLabel.getWidth() - cornerSize &&
                        e.getY() > cardPhotoLabel.getHeight() - cornerSize) {
                    cardPhotoLabel.setCursor(new Cursor(Cursor.SE_RESIZE_CURSOR));
                }
                else { cardPhotoLabel.setCursor(new Cursor(Cursor.HAND_CURSOR)); }
            }
        });
        frontCardPanel.add(cardPhotoLabel);

        int y = 200;
        if ("Student".equals(templateType)) {
            y = addCardLabel(frontCardPanel, frontCardLabels, "name", 50, y, 450, 30);
            y = addCardLabel(frontCardPanel, frontCardLabels, "class", 50, y, 450, 25);
            y = addCardLabel(frontCardPanel, frontCardLabels, "section", 50, y, 450, 25);
            y = addCardLabel(frontCardPanel, frontCardLabels, "department", 50, y, 450, 25);
            y = addCardLabel(frontCardPanel, frontCardLabels, "organization", 50, y, 450, 25);
        } else if ("Business".equals(templateType)) {
            y = addCardLabel(frontCardPanel, frontCardLabels, "name", 50, y, 450, 30);
            y = addCardLabel(frontCardPanel, frontCardLabels, "email", 50, y, 450, 25);
            y = addCardLabel(frontCardPanel, frontCardLabels, "contact", 50, y, 450, 25);
            y = addCardLabel(frontCardPanel, frontCardLabels, "employeeId", 50, y, 450, 25);
            y = addCardLabel(frontCardPanel, frontCardLabels, "designation", 50, y, 450, 25);
            y = addCardLabel(frontCardPanel, frontCardLabels, "organization", 50, y, 450, 25);
        } else if ("Hospital".equals(templateType)) {
            y = addCardLabel(frontCardPanel, frontCardLabels, "name", 50, y, 450, 30);
            y = addCardLabel(frontCardPanel, frontCardLabels, "email", 50, y, 450, 25);
            y = addCardLabel(frontCardPanel, frontCardLabels, "contact", 50, y, 450, 25);
            y = addCardLabel(frontCardPanel, frontCardLabels, "specialization", 50, y, 450, 25);
            y = addCardLabel(frontCardPanel, frontCardLabels, "organization", 50, y, 450, 25);
        }
    }

    /**
     * Sets up the back card panel with additional information and signature
     */
    private void setupBackCard() {
        int y = 80;
        if ("Student".equals(templateType)) {
            y = addCardLabel(backCardPanel, backCardLabels, "issueDate", 50, y, 450, 30);
            y = addCardLabel(backCardPanel, backCardLabels, "expiryDate", 50, y, 450, 30);
            y = addCardLabel(backCardPanel, backCardLabels, "contact", 50, y, 450, 30);
            y = addCardLabel(backCardPanel, backCardLabels, "address", 50, y, 450, 30);
        } else if ("Business".equals(templateType)) {
            y = addCardLabel(backCardPanel, backCardLabels, "organization", 50, y, 450, 30);
            y = addCardLabel(backCardPanel, backCardLabels, "issueDate", 50, y, 450, 30);
            y = addCardLabel(backCardPanel, backCardLabels, "expiryDate", 50, y, 450, 30);
            y = addCardLabel(backCardPanel, backCardLabels, "address", 50, y, 450, 30);
            y = addCardLabel(backCardPanel, backCardLabels, "termsConditions", 50, y, 450, 30);
        } else if ("Hospital".equals(templateType)) {
            y = addCardLabel(backCardPanel, backCardLabels, "organization", 50, y, 450, 30);
            y = addCardLabel(backCardPanel, backCardLabels, "issueDate", 50, y, 450, 30);
            y = addCardLabel(backCardPanel, backCardLabels, "expiryDate", 50, y, 450, 30);
            y = addCardLabel(backCardPanel, backCardLabels, "address", 50, y, 450, 30);
        }

        signatureLabel = new JLabel();
        signatureLabel.setBounds(350, 250, 150, 80);
        signatureLabel.setBorder(BorderFactory.createLineBorder(ACCENT_GREEN, 1));
        signatureLabel.setOpaque(false);
        backCardPanel.add(signatureLabel);
    }

    /**
     * Adds a draggable label to the card preview
     */
    private int addCardLabel(JPanel panel, Map<String, JLabel> labelMap, String fieldName, int x, int y, int width, int height) {
        JLabel label = new JLabel("", SwingConstants.CENTER);
        label.setBounds(x, y, width, height);
        label.setFont(new Font("Playfair Display", Font.PLAIN, 16));
        label.setForeground(TEXT_DARK);
        label.setCursor(new Cursor(Cursor.MOVE_CURSOR));

        LabelStyle style = new LabelStyle();
        labelStyles.put(fieldName, style);

        final Point[] dragStart = {null};
        label.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragStart[0] = e.getPoint();
                selectedLabel = label;
                labelFontCombo.setSelectedItem(style.fontName);
                labelSizeCombo.setSelectedItem(String.valueOf(style.fontSize));
                labelFontCombo.setEnabled(true);
                labelSizeCombo.setEnabled(true);
                labelColorBtn.setEnabled(true);
            }
            public void mouseReleased(MouseEvent e) { dragStart[0] = null; }
        });

        label.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (dragStart[0] != null) {
                    int dx = e.getX() - dragStart[0].x;
                    int dy = e.getY() - dragStart[0].y;
                    Point loc = label.getLocation();
                    int newX = Math.max(0, Math.min(panel.getWidth() - label.getWidth(), loc.x + dx));
                    int newY = Math.max(0, Math.min(panel.getHeight() - label.getHeight(), loc.y + dy));
                    label.setLocation(newX, newY);
                }
            }
        });

        panel.add(label);
        labelMap.put(fieldName, label);
        return y + height + 5;
    }

    /**
     * Updates the card preview with current field values and styling
     */
    private void updateCardPreview() {
        if (frontCardLabels.isEmpty() && backCardLabels.isEmpty()) return;

        for (Map.Entry<String, JLabel> entry : frontCardLabels.entrySet()) {
            String fieldName = entry.getKey();
            JLabel label = entry.getValue();
            String text = getFieldValue(fieldName);
            label.setText(text);
            LabelStyle style = labelStyles.get(fieldName);
            if (style != null) {
                Font font = new Font(style.fontName, fieldName.equals("name") ? Font.BOLD : Font.PLAIN, style.fontSize);
                label.setFont(font);
                label.setForeground(style.color);
                FontMetrics fm = label.getFontMetrics(font);
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getHeight();
                if (textWidth > 0) { label.setSize(Math.min(textWidth + 20, 500), Math.max(textHeight + 5, 25)); }
            }
        }

        for (Map.Entry<String, JLabel> entry : backCardLabels.entrySet()) {
            String fieldName = entry.getKey();
            JLabel label = entry.getValue();
            String text = getFieldValue(fieldName);
            label.setText(text);
            LabelStyle style = labelStyles.get(fieldName);
            if (style != null) {
                Font font = new Font(style.fontName, Font.PLAIN, style.fontSize);
                label.setFont(font);
                label.setForeground(style.color);
                FontMetrics fm = label.getFontMetrics(font);
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getHeight();
                if (textWidth > 0) { label.setSize(Math.min(textWidth + 20, 500), Math.max(textHeight + 5, 25)); }
            }
        }

        if (profileImage != null && cardPhotoLabel != null) {
            Dimension size = cardPhotoLabel.getSize();
            ImageIcon icon = new ImageIcon(getScaledImage(profileImage, size.width, size.height));
            cardPhotoLabel.setIcon(icon);
            cardPhotoLabel.setText("");
        }

        if (signatureImage != null && signatureLabel != null) {
            ImageIcon sigIcon = new ImageIcon(getScaledImage(signatureImage, 150, 80));
            signatureLabel.setIcon(sigIcon);
        } else if (signatureLabel != null) { signatureLabel.setIcon(null); }

        if (frontCardPanel != null) {
            frontCardPanel.revalidate();
            frontCardPanel.repaint();
        }
        if (backCardPanel != null) {
            backCardPanel.revalidate();
            backCardPanel.repaint();
        }
    }

    /**
     * Creates a scaled version of an image with high-quality rendering
     */
    private java.awt.Image getScaledImage(BufferedImage src, int w, int h) {
        BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();
        return resized;
    }

    /**
     * Gets the formatted value of a field for display on the card
     */
    private String getFieldValue(String fieldName) {
        JComponent comp = inputFields.get(fieldName);
        if (comp == null) return "";
        if (comp instanceof JTextField) {
            String value = ((JTextField) comp).getText();
            return formatFieldLabel(fieldName, value);
        } else if (comp instanceof JDateChooser) {
            Date date = ((JDateChooser) comp).getDate();
            if (date != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                return formatFieldLabel(fieldName, sdf.format(date));
            }
        }
        return "";
    }

    /**
     * Formats field values with appropriate labels for display
     */
    private String formatFieldLabel(String fieldName, String value) {
        if (value.isEmpty()) return "";
        switch (fieldName) {
            case "name":
            case "designation":
            case "organization":
                return value;
            case "class":
                return "Class: " + value;
            case "section":
                return "Section: " + value;
            case "department":
                return "Department: " + value;
            case "email":
                return "Email: " + value;
            case "contact":
                return "Contact: " + value;
            case "employeeId":
                return "Employee ID: " + value;
            case "specialization":
                return "Specialization: " + value;
            case "issueDate":
                return "Issue Date: " + value;
            case "expiryDate":
                return "Expiry Date: " + value;
            case "address":
                return "Address: " + value;
            case "termsConditions":
                return "Terms & Conditions: " + value;
            default:
                return value;
        }
    }

    /**
     * Captures the current card design as images for saving
     */
    private void captureCardImages() {
        if (frontCardPanel != null) {
            generatedFrontImage = new BufferedImage(
                    frontCardPanel.getWidth(),
                    frontCardPanel.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );
            Graphics2D g2 = generatedFrontImage.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            frontCardPanel.paintAll(g2);
            g2.dispose();
        }
        if (backCardPanel != null) {
            generatedBackImage = new BufferedImage(
                    backCardPanel.getWidth(),
                    backCardPanel.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );
            Graphics2D g2 = generatedBackImage.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            backCardPanel.paintAll(g2);
            g2.dispose();
        }
    }

    /**
     * Creates the preview page for generated ID cards
     */
    private JPanel createPreviewPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_LIGHT);

        // Navigation bar for preview page
        JPanel navBar = new JPanel(new BorderLayout());
        navBar.setBackground(NAV_BAR);
        navBar.setBorder(BorderFactory.createLineBorder(ACCENT_GREEN, 2));
        navBar.setPreferredSize(new Dimension(0, 80));
        JButton backToEditBtn = createRoundedButton("← Back to Edit", 200, 45);
        backToEditBtn.addActionListener(e -> {
            int option = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you wanna go back?",
                    "Confirm",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (option == JOptionPane.YES_OPTION) { cardLayout.show(mainPanel, "IDCard"); }
        });
        JLabel titleLabel = new JLabel("★ Your Generated ID Card! ★", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Playfair Display", Font.BOLD, 32));
        titleLabel.setForeground(TEXT_LIGHT);
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        leftPanel.setOpaque(false);
        leftPanel.add(backToEditBtn);
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 15));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(240, 0));
        navBar.add(leftPanel, BorderLayout.WEST);
        navBar.add(titleLabel, BorderLayout.CENTER);
        navBar.add(rightPanel, BorderLayout.EAST);
        panel.add(navBar, BorderLayout.NORTH);

        // Center panel with card previews
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(BG_LIGHT);

        // Front side preview
        JLabel frontTitle = new JLabel("♢ Front Side ♢");
        frontTitle.setFont(new Font("Playfair Display", Font.BOLD, 20));
        frontTitle.setForeground(TEXT_DARK);
        frontTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(frontTitle);
        centerPanel.add(Box.createVerticalStrut(10));
        previewFrontLabel = new JLabel();
        previewFrontLabel.setBorder(new LineBorder(ACCENT_GREEN, 3));
        previewFrontLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(previewFrontLabel);
        centerPanel.add(Box.createVerticalStrut(30));

        // Back side preview
        JLabel backTitle = new JLabel("♢ Back Side ♢");
        backTitle.setFont(new Font("Playfair Display", Font.BOLD, 20));
        backTitle.setForeground(TEXT_DARK);
        backTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(backTitle);
        centerPanel.add(Box.createVerticalStrut(10));
        previewBackLabel = new JLabel();
        previewBackLabel.setBorder(new LineBorder(ACCENT_GREEN, 3));
        previewBackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(previewBackLabel);

        JScrollPane scrollPane = new JScrollPane(centerPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Action buttons for export and navigation
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        buttonPanel.setBackground(BG_LIGHT);
        JButton savePNG = createRoundedButton("◆ Export as Image", 220, 50);
        JButton savePDF = createRoundedButton("◆ Export as PDF", 220, 50);
        JButton backBtn = createRoundedButton("⌂ Back to Menu", 220, 50);
        buttonPanel.add(savePNG);
        buttonPanel.add(savePDF);
        buttonPanel.add(backBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "Selection"));
        savePNG.addActionListener(e -> saveAsImage());
        savePDF.addActionListener(e -> saveAsPDF());
        return panel;
    }

    /**
     * Updates the preview images in the preview page
     */
    private void updatePreviewImages() {
        if (previewFrontLabel != null && generatedFrontImage != null) {
            previewFrontLabel.setIcon(new ImageIcon(generatedFrontImage));
        }
        if (previewBackLabel != null && generatedBackImage != null) {
            previewBackLabel.setIcon(new ImageIcon(generatedBackImage));
        }
    }

    /**
     * Saves the generated ID card as PNG image files
     */
    private void saveAsImage() {
        if (generatedFrontImage == null) {
            JOptionPane.showMessageDialog(this, "No card to save!");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(templateType + "_IDCard_Front.png"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File frontFile = chooser.getSelectedFile();
                ImageIO.write(generatedFrontImage, "png", frontFile);
                String backPath = frontFile.getAbsolutePath().replace("_Front.png", "_Back.png");
                if (generatedBackImage != null) { ImageIO.write(generatedBackImage, "png", new File(backPath)); }
                JOptionPane.showMessageDialog(this, "ID Card saved successfully!");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error saving card!");
            }
        }
    }

    /**
     * Saves the generated ID card as a PDF document
     */
    private void saveAsPDF() {
        if (generatedFrontImage == null) {
            JOptionPane.showMessageDialog(this, "No card to save!");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(templateType + "_IDCard.pdf"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Document doc = new Document();
                PdfWriter.getInstance(doc, new java.io.FileOutputStream(chooser.getSelectedFile()));
                doc.open();

                File tempFront = File.createTempFile("idcard_front", ".png");
                ImageIO.write(generatedFrontImage, "png", tempFront);
                Image pdfFront = Image.getInstance(tempFront.getAbsolutePath());
                pdfFront.scaleToFit(500, 400);
                doc.add(pdfFront);
                tempFront.delete();

                doc.newPage();
                if (generatedBackImage != null) {
                    File tempBack = File.createTempFile("idcard_back", ".png");
                    ImageIO.write(generatedBackImage, "png", tempBack);
                    Image pdfBack = Image.getInstance(tempBack.getAbsolutePath());
                    pdfBack.scaleToFit(500, 400);
                    doc.add(pdfBack);
                    tempBack.delete();
                }

                doc.close();
                JOptionPane.showMessageDialog(this, "ID Card PDF saved successfully!");

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error saving PDF: " + ex.getMessage());
            }
        }
    }

    /**
     * Creates a custom rounded button with hover effects
     */
    private JButton createRoundedButton(String text, int width, int height) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) { g2.setColor(new Color(60, 70, 130)); }
                else if (getModel().isRollover()) { g2.setColor(BUTTON_HOVER); }
                else { g2.setColor(getBackground()); }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 50, 50);
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(getText());
                int textHeight = fm.getAscent();
                g2.drawString(getText(), (getWidth() - textWidth) / 2, (getHeight() + textHeight) / 2 - 2);
                g2.dispose();
            }
        };
        button.setPreferredSize(new Dimension(width, height));
        button.setBackground(BUTTON_COLOR);
        button.setForeground(TEXT_LIGHT);
        button.setFont(new Font("Playfair Display", Font.BOLD, 16));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(BUTTON_HOVER);
                button.repaint();
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(BUTTON_COLOR);
                button.repaint();
            }
        });
        return button;
    }

    /**
     * Creates a selection button for template choices
     */
    private JButton createSelectionButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) { g2.setColor(new Color(60, 70, 130)); }
                else if (getModel().isRollover()) { g2.setColor(BUTTON_HOVER); }
                else { g2.setColor(getBackground()); }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 60, 60);
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(getText());
                int textHeight = fm.getAscent();
                g2.drawString(getText(), (getWidth() - textWidth) / 2, (getHeight() + textHeight) / 2 - 2);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(450, 70));
        btn.setBackground(ACCENT_BLUE);
        btn.setForeground(TEXT_LIGHT);
        btn.setFont(new Font("Playfair Display", Font.BOLD, 26));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(BUTTON_HOVER);
                btn.repaint();
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(ACCENT_BLUE);
                btn.repaint();
            }
        });
        return btn;
    }

    /**
     * Main method to launch the application
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(IDMateApp::new);
    }

    /**
     * Custom signature panel for capturing handwritten signatures
     */
    class SignaturePanel extends JPanel {
        private java.util.List<Point> points = new ArrayList<>();
        private Point lastPoint = null;

        public SignaturePanel() {
            setBackground(Color.WHITE);
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    lastPoint = e.getPoint();
                    points.add(lastPoint);
                }
                public void mouseReleased(MouseEvent e) {
                    lastPoint = null;
                    points.add(null);
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    Point currentPoint = e.getPoint();
                    if (lastPoint != null) {
                        Graphics2D g2 = (Graphics2D) getGraphics();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(TEXT_DARK);
                        g2.setStroke(new BasicStroke(2));
                        g2.drawLine(lastPoint.x, lastPoint.y, currentPoint.x, currentPoint.y);
                        g2.dispose();
                    }
                    points.add(currentPoint);
                    lastPoint = currentPoint;
                }
            });
        }

        public void clear() {
            points.clear();
            lastPoint = null;
            repaint();
        }

        public BufferedImage getSignatureImage() {
            BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 0));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(TEXT_DARK);
            g2.setStroke(new BasicStroke(2));
            Point prev = null;
            for (Point p : points) {
                if (p == null) { prev = null; }
                else if (prev != null) {
                    g2.drawLine(prev.x, prev.y, p.x, p.y);
                    prev = p;
                } else { prev = p; }
            }
            g2.dispose();
            return img;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TEXT_DARK);
            g2.setStroke(new BasicStroke(2));
            Point prev = null;
            for (Point p : points) {
                if (p == null) { prev = null; }
                else if (prev != null) {
                    g2.drawLine(prev.x, prev.y, p.x, p.y);
                    prev = p;
                } else { prev = p; }
            }
        }
    }
}