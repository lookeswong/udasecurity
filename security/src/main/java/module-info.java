module com.udacity.security {
    requires miglayout;
    requires com.google.gson;
    requires com.google.common;
    requires java.desktop;
    requires java.prefs;
    requires com.udacity.image;
    opens com.udacity.security.data to com.google.gson;
}