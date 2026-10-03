import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;

public class TestFallback {
    public static void main(String[] args) {
        for (java.lang.reflect.Field field : MediaCodecSelector.class.getFields()) {
            System.out.println(field.getName());
        }
    }
}
