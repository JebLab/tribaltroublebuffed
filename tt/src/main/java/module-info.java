module com.oddlabs.tt {
    requires com.oddlabs.common;
    requires org.joml;
    requires static org.jspecify;
    requires java.desktop;
    requires java.xml;
    requires java.logging;
    requires org.lwjgl;
    requires org.lwjgl.glfw;
    requires org.lwjgl.openal;
    requires org.lwjgl.opengl;
    requires org.lwjgl.stb;
    requires org.lwjgl.tinyfd;

    // Ruleset data files are read into the records of this package.
    opens com.oddlabs.tt.ruleset to com.fasterxml.jackson.databind;
}
