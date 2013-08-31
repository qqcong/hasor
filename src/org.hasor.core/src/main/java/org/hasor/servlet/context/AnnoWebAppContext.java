/*
 * Copyright 2008-2009 the original ������(zyc@hasor.net).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hasor.servlet.context;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import javax.servlet.ServletContext;
import org.hasor.context.Environment;
import org.hasor.context.ModuleInfo;
import org.hasor.context.Settings;
import org.hasor.context.anno.context.AnnoAppContext;
import org.hasor.context.environment.StandardEnvironment;
import org.hasor.servlet.binder.FilterPipeline;
import org.hasor.servlet.binder.SessionListenerPipeline;
import org.hasor.servlet.binder.support.ManagedErrorPipeline;
import org.hasor.servlet.binder.support.ManagedFilterPipeline;
import org.hasor.servlet.binder.support.ManagedServletPipeline;
import org.hasor.servlet.binder.support.ManagedSessionListenerPipeline;
import org.hasor.servlet.binder.support.WebApiBinderModule;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
/**
 * 
 * @version : 2013-7-16
 * @author ������ (zyc@hasor.net)
 */
public class AnnoWebAppContext extends AnnoAppContext {
    //
    public AnnoWebAppContext(ServletContext servletContext) throws IOException {
        super("hasor-config.xml", servletContext);
    }
    public AnnoWebAppContext(String mainConfig, ServletContext servletContext) throws IOException {
        super(mainConfig, servletContext);
    }
    /**��ȡ{@link ServletContext}*/
    public ServletContext getServletContext() {
        if (this.getContext() instanceof ServletContext)
            return (ServletContext) this.getContext();
        else
            return null;
    }
    protected Environment createEnvironment() {
        return new WebStandardEnvironment(this.getSettings(), this.getServletContext());
    }
    protected Injector createInjector(Module[] guiceModules) {
        Module webModule = new Module() {
            public void configure(Binder binder) {
                /*Bind*/
                binder.bind(ManagedErrorPipeline.class);
                binder.bind(ManagedServletPipeline.class);
                binder.bind(FilterPipeline.class).to(ManagedFilterPipeline.class);
                binder.bind(SessionListenerPipeline.class).to(ManagedSessionListenerPipeline.class);
                /*��ServletContext�����Provider*/
                binder.bind(ServletContext.class).toProvider(new Provider<ServletContext>() {
                    public ServletContext get() {
                        return getServletContext();
                    }
                });
            }
        };
        //2.
        ArrayList<Module> guiceModuleSet = new ArrayList<Module>();
        guiceModuleSet.add(webModule);
        if (guiceModules != null)
            for (Module mod : guiceModules)
                guiceModuleSet.add(mod);
        return super.createInjector(guiceModuleSet.toArray(new Module[guiceModuleSet.size()]));
    }
    protected WebApiBinderModule newApiBinder(final ModuleInfo forModule, final Binder binder) {
        return new WebApiBinderModule(this, forModule) {
            public Binder getGuiceBinder() {
                return binder;
            }
        };
    }
}
/**
 * ����ע��MORE_WEB_ROOT���������Լ�Web����������ά����
 * @version : 2013-7-17
 * @author ������ (zyc@hasor.net)
 */
class WebStandardEnvironment extends StandardEnvironment {
    private ServletContext servletContext;
    public WebStandardEnvironment(Settings settings, ServletContext servletContext) {
        super(settings);
        this.servletContext = servletContext;
    }
    protected Map<String, String> configEnvironment() {
        Map<String, String> hasorEnv = super.configEnvironment();
        String webContextDir = servletContext.getRealPath("/");
        hasorEnv.put("HASOR_WEBROOT", webContextDir);
        //
        /*��������work_home*/
        String workDir = this.getSettings().getString("environmentVar.HASOR_WORK_HOME", "./");
        workDir = workDir.replace("/", File.separator);
        if (workDir.startsWith("." + File.separatorChar))
            hasorEnv.put("HASOR_WORK_HOME", new File(webContextDir, workDir.substring(2)).getAbsolutePath());
        return hasorEnv;
    }
}