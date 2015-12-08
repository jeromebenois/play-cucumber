package play.cucumber.formatters;

import java.io.IOException;

public class ConsoleAppendable implements Appendable {
    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        System.out.append(csq, start, end);
        return this;
    }

    @Override
    public Appendable append(char c) throws IOException {
        System.out.append(c);
        return this;
    }

    @Override
    public Appendable append(CharSequence csq) throws IOException {
        System.out.append(csq);
        return this;
    }
}
