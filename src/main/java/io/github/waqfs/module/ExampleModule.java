package io.github.waqfs.module;

public class ExampleModule extends BaseModule {
    protected static final String MODULE_NAME = "Example";
    protected static final String MODULE_TOOLTIP = "This is an example toggle.";
    protected static final String MODULE_ID = "example";

    public ExampleModule() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
    }

    @Override
    protected void whenEnabled() {
        System.out.println("Example module enabled.");
    }

    @Override
    protected void whenDisabled() {
        System.out.println("Example module disabled.");
    }
}
