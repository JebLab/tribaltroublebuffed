package com.oddlabs.tt.audio;

import com.oddlabs.tt.global.Headless;
import com.oddlabs.tt.resource.File;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;

public final class AudioFile extends File<Audio> {
    /** What every sound loads as in a headless world, which has no audio device. */
    private static final @NonNull Audio SILENT = new Audio() {
    };

    public AudioFile(@NonNull String location) {
        super(location);
    }

    @Override
    public @NonNull Audio get() throws UncheckedIOException {
        if (Headless.isEnabled())
            return SILENT;
        try {
            return AudioManager.getManager().createAudio(getURL());
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not load " + this.getURL(), ex);
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof AudioFile && super.equals(o);
    }
}
