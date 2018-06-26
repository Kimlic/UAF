/*
Copyright 2009-2018 Igor Polevoy
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.ebayopensource.fidouaf.res.util;

import java.io.IOException;
import javax.servlet.*;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.web.ActiveJdbcFilter;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class DBFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveJdbcFilter.class);

    private String driver;
    private String url;
    private String user;
    private String password;

    @Override
    public void init(FilterConfig config) throws ServletException {

//        jndiName = config.getInitParameter("jndiName");
//        if(jndiName == null)
//            throw new IllegalArgumentException("must provide jndiName parameter for this filter");
//
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        long before = System.currentTimeMillis();
        try{
            Base.open("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/fido", "postgres", "");
            Base.openTransaction();
            chain.doFilter(req, resp);
            Base.commitTransaction();
        }
        catch (IOException e){
            Base.rollbackTransaction();
            throw e;
        }
        catch (ServletException e){
            Base.rollbackTransaction();
            throw e;
        }
        finally{

            Base.close();
        }
        LOGGER.info("Processing took: {} milliseconds", System.currentTimeMillis() - before);
    }

    @Override
    public void destroy() {}
}
