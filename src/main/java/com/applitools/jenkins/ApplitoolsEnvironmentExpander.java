package com.applitools.jenkins;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by addihorowitz on 5/7/17.
 */
public class ApplitoolsEnvironmentExpander extends EnvironmentExpander {
    private static final long serialVersionUID = 1;
    private final Map<String, String> overrides;

    ApplitoolsEnvironmentExpander(HashMap<String, String> overrides) {
        this.overrides = overrides;

    }

    @Override
    public void expand(@NonNull EnvVars env) throws IOException, InterruptedException {
        env.overrideAll(overrides);
    }
}
