/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.plugin.lucene;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.container.Container;
import org.xwiki.container.daemon.DaemonContainerException;
import org.xwiki.container.daemon.DaemonContainerInitializer;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;

/**
 * Common methods for all XWiki thread classes in the Lucene plugin.
 */
public abstract class AbstractXWikiRunnable implements Runnable
{
    /**
     * Logging object.
     */
    private static final Log LOG = LogFactory.getLog(IndexUpdater.class);

    protected void initXWikiContainer(XWikiContext context)
    {
        DaemonContainerInitializer dci = (DaemonContainerInitializer) Utils.getComponent(
            DaemonContainerInitializer.ROLE, context);

        try {
            // Initialize the Container objects
            dci.initializeRequest();
        } catch (DaemonContainerException e) {
            // Note: We should raise an exception here but we cannot since XWikiDefaultPlugin has overrident's 
            // XWikiPluginInterface's init() method without declaring a throw XWikiException...
            LOG.error("Failed to initialize request. Behavior of the Lucene plugin could be "
                + "instable. We recommend stopping the container, fixing the issue and "
                + "restarting it.", e);
        }

        // This is a bridge that we need for old code to play well with new components.
        // Old code relies on the XWikiContext object whereas new code uses the Container component.
        Container container = (Container) Utils.getComponent(Container.ROLE, context);
        container.getRequest().setProperty("xwikicontext", context);
    }
    
    protected void cleanupXWikiContainer(XWikiContext context)
    {
        Container container = (Container) Utils.getComponent(Container.ROLE, context);
        // We must ensure we clean the ThreadLocal variables located in the Container 
        // component as otherwise we will have a potential memory leak.
        container.removeRequest();
    }
}
