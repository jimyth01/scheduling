/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2014 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ActiveEon Team
 *                        http://www.activeeon.com/
 *  Contributor(s):
 *
 * ################################################################
 * $$ACTIVEEON_INITIAL_DEV$$
 */
package org.ow2.proactive.scheduler.rest.ds;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.ow2.proactive.scheduler.common.exception.NotConnectedException;
import org.ow2.proactive.scheduler.common.exception.PermissionException;
import org.ow2.proactive.scheduler.common.task.dataspaces.RemoteSpace;
import org.ow2.proactive_grid_cloud_portal.dataspace.dto.ListFile;

import com.sun.msv.datatype.xsd.ListType;


public interface IDataSpaceClient {

    public enum Dataspace {
        USER("user"), GLOBAL("global");

        private String value;

        Dataspace(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    public boolean download(IRemoteSource source, ILocalDestination destination)
            throws NotConnectedException, PermissionException;

    public boolean upload(ILocalSource source, IRemoteDestination destination) throws NotConnectedException,
            PermissionException;

    /**
     * Returns a {@link ListType} type object which contains the names of files
     * and directories in the specified location the <i>dataspace</i>.
     *
     * @throws NotConnectedException
     *             if the client is not logged in or the session has expired
     * @throws PermissionException
     *             if the user does not have permission to upload the file to
     *             the specified location in the server
     */
    public ListFile list(IRemoteSource source) throws NotConnectedException, PermissionException;

    /**
     * Deletes the specified directory or the file from the <i>dataspace</i>.
     *
     * @throws NotConnectedException
     *             if the client is not logged in or the session has expired
     * @throws PermissionException
     *             if the user does not have permission to upload the file to
     *             the specified location in the server
     */
    public boolean delete(IRemoteSource source) throws NotConnectedException, PermissionException;

    /**
     * Returns the metadata map of the specified file in the <i>dataspace</i>.
     *
     * @return an instace of {@link Map}
     * @throws NotConnectedException
     *             if the client is not logged in or the session has expired
     * @throws PermissionException
     *             if the user does not have permission to upload the file to
     *             the specified location in the server
     */
    public Map<String, String> metadata(IRemoteSource source) throws NotConnectedException,
            PermissionException;

    /**
     * Returns a {@link RemoteSpace} implementation instance which represents
     * the <i>globalspace</i>.
     *
     * @return an instance of {@link RemoteSpace}
     */
    public RemoteSpace getGlobalSpace();

    /**
     * Returns a {@link RemoteSpace} implementation instance which represents
     * the <i>userspace</i>
     *
     * @return an instance of {@link RemoteSpace}
     */
    public RemoteSpace getUserSpace();

    public interface IRemoteSource {
        public Dataspace getDataspace();

        public String getPath();

        public List<String> getIncludes();

        public List<String> getExcludes();
    }

    public interface ILocalDestination {
        public void readFrom(InputStream is, String encoding) throws IOException;
    }

    public interface ILocalSource {
        public void writeTo(OutputStream outputStream) throws IOException;

        public String getEncoding() throws IOException;
    }

    public interface IRemoteDestination {
        public Dataspace getDataspace();

        public String getPath();
    }

}
