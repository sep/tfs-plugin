package hudson.plugins.tfs.commands;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissions;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import hudson.plugins.tfs.IntegrationTestHelper;
import hudson.plugins.tfs.IntegrationTests;
import hudson.plugins.tfs.commands.ListWorkspacesCommand.WorkspaceFactory;
import hudson.plugins.tfs.model.Server;
import hudson.plugins.tfs.model.Workspace;
import hudson.plugins.tfs.model.Workspaces;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.jvnet.hudson.test.Bug;
import org.mockito.Mockito;

public class ListWorkspacesCommandTest extends AbstractCallableCommandTest {

    private WorkspaceFactory factory;

    @Before
    public void initialize() {
        factory = new Workspaces(server);
    }

    @Category(IntegrationTests.class)
    @Test public void assertLoggingWithComputer() throws Exception {
        final IntegrationTestHelper helper = new IntegrationTestHelper();
        final String serverUrl = helper.getServerUrl();
        final URI serverUri = URI.create(serverUrl);
        Server.ensureNativeLibrariesConfigured();
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                IntegrationTestHelper.TestUserName, IntegrationTestHelper.TestUserPassword);
        final TFSTeamProjectCollection tpc = new TFSTeamProjectCollection(serverUri, credentials);

        try {
            final VersionControlClient vcc = tpc.getVersionControlClient();
            final com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace[] workspaces
                    = new com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace[1];
            workspaces[0] = new com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace(
                    vcc,
                    "Hudson.JOBXXXXXXXXXXXXXX",
                    "First.LastXX",
                    "This is a comment",
                    null,
                    "XXXX-XXXX-007",
                    WorkspaceLocation.SERVER
            );
            when(server.getUrl()).thenReturn("http://tfs.invalid:8080/tfs/DefaultCollection/");
            when(this.vcc.queryWorkspaces(null, null, "XXXX-XXXX-007", WorkspacePermissions.NONE_OR_NOT_SUPPORTED))
                    .thenReturn(workspaces);
            final ListWorkspacesCommand command = new ListWorkspacesCommand(factory, server, "XXXX-XXXX-007");
            final Callable<List<Workspace>> callable = command.getCallable();

            callable.call();

            assertLog(
                "Listing workspaces from http://tfs.invalid:8080/tfs/DefaultCollection/...",
                "Workspace                Owner        Computer      Comment          ",
                "------------------------ ------------ ------------- -----------------",
                "Hudson.JOBXXXXXXXXXXXXXX First.LastXX XXXX-XXXX-007 This is a comment"
            );
        } finally {
            tpc.close();
        }
    }

    @Category(IntegrationTests.class)
    @Test public void assertLoggingWithoutComputer() throws Exception {
        final IntegrationTestHelper helper = new IntegrationTestHelper();
        final String serverUrl = helper.getServerUrl();
        final URI serverUri = URI.create(serverUrl);
        Server.ensureNativeLibrariesConfigured();
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                IntegrationTestHelper.TestUserName, IntegrationTestHelper.TestUserPassword);
        final TFSTeamProjectCollection tpc = new TFSTeamProjectCollection(serverUri, credentials);

        try {
            final VersionControlClient vcc = tpc.getVersionControlClient();
            final com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace[] workspaces
                    = new com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace[1];
            workspaces[0] = new com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace(
                    vcc,
                    "Hudson.JOBXXXXXXXXXXXXXX",
                    "First.LastXX",
                    "This is a comment",
                    null,
                    "XXXX-XXXX-007",
                    WorkspaceLocation.SERVER
            );
            when(server.getUrl()).thenReturn("http://tfs.invalid:8080/tfs/DefaultCollection/");
            when(this.vcc.queryWorkspaces(null, null, null, WorkspacePermissions.NONE_OR_NOT_SUPPORTED))
                    .thenReturn(workspaces);
            final ListWorkspacesCommand command = new ListWorkspacesCommand(factory, server, null);
            final Callable<List<Workspace>> callable = command.getCallable();

            callable.call();

            assertLog(
                    "Listing workspaces from http://tfs.invalid:8080/tfs/DefaultCollection/...",
                    "Workspace                Owner        Computer      Comment          ",
                    "------------------------ ------------ ------------- -----------------",
                    "Hudson.JOBXXXXXXXXXXXXXX First.LastXX XXXX-XXXX-007 This is a comment"
            );
        } finally {
            tpc.close();
        }
    }

    @Test
    public void assertEmptyListWithEmptyOutput() throws Exception {
        ListWorkspacesCommand command = new ListWorkspacesCommand(null, mock(Server.class));
        List<Workspace> list = command.parse(new StringReader(""));
        assertNotNull("List can not be null", list);
        assertEquals("Number of workspaces was incorrect", 0, list.size());
    }

    @Test
    public void assertFactoryIsUsedToCreateWorkspaces() throws Exception {
        WorkspaceFactory factory = Mockito.mock(ListWorkspacesCommand.WorkspaceFactory.class);
        
        StringReader reader = new StringReader(
                "Server: https://tfs02.codeplex.com/\n" +
                "Workspace Owner          Computer Comment\n" +
                "--------- -------------- -------- ----------------------------------------------------------------------------------------------------------\n" +
                "\n" +
                "asterix2  SND\\redsolo_cp ASTERIX\n");

        new ListWorkspacesCommand(factory, mock(Server.class)).parse(reader);
        Mockito.verify(factory).createWorkspace("asterix2", "ASTERIX", "SND\\redsolo_cp", "");
    }

    @Test
    public void assertListWithValidOutput() throws Exception {
        StringReader reader = new StringReader(
                "Server: https://tfs02.codeplex.com/\n" +
                "Workspace Owner          Computer Comment\n" +
                "--------- -------------- -------- ----------------------------------------------------------------------------------------------------------\n" +
                "\n" +
                "asterix2  SND\\redsolo_cp ASTERIX\n" +
                "astreix   SND\\redsolo_cp ASTERIX  This is a comment\n");
        
        ListWorkspacesCommand command = new ListWorkspacesCommand(
                new Workspaces(Mockito.mock(Server.class)), 
                mock(Server.class));
        List<Workspace> list = command.parse(reader);
        assertNotNull("List can not be null", list);
        assertEquals("Number of workspaces was incorrect", 2, list.size());
        Workspace workspace = list.get(0);
        assertEquals("The workspace name is incorrect", "asterix2", workspace.getName());
        assertEquals("The owner name is incorrect", "SND\\redsolo_cp", workspace.getOwner());
        assertEquals("The computer name is incorrect", "ASTERIX", workspace.getComputer());
        workspace = list.get(1);
        assertEquals("The workspace name is incorrect", "astreix", workspace.getName());
        assertEquals("The owner name is incorrect", "SND\\redsolo_cp", workspace.getOwner());
        assertEquals("The computer name is incorrect", "ASTERIX", workspace.getComputer());
        assertEquals("The comment is incorrect", "This is a comment", workspace.getComment());
    }

    @Test
    public void assertListWithWorkspaceContainingSpace() throws Exception {
        StringReader reader = new StringReader(
                "Server: https://tfs02.codeplex.com/\n" +
                "Workspace          Owner      Computer Comment\n" +
                "------------------ ---------- -------- ----------------------------------------\n" +
                "Hudson-node lookup redsolo_cp ASTERIX\n");
        
        ListWorkspacesCommand command = new ListWorkspacesCommand(
                new Workspaces(Mockito.mock(Server.class)), 
                mock(Server.class));
        List<Workspace> list = command.parse(reader);
        assertNotNull("List can not be null", list);
        assertEquals("Number of workspaces was incorrect", 1, list.size());
        Workspace workspace = list.get(0);
        assertEquals("The workspace name is incorrect", "Hudson-node lookup", workspace.getName());
    }

    @Bug(4666)
    @Test
    public void assertNoIndexOutOfBoundsIsThrown() throws Exception {
        WorkspaceFactory factory = Mockito.mock(ListWorkspacesCommand.WorkspaceFactory.class);
        
        StringReader reader = new StringReader(
                "Server: teamserver-01\n" +
                "Workspace         Owner  Computer    Comment\n" +
                "----------------- ------ ----------- ------------------------------------------\n" +
                "Hudson-Scrumboard dennis W7-DENNIS-1\n" + 
                "W7-DENNIS-1       dennis W7-DENNIS-1\n");

        new ListWorkspacesCommand(factory, mock(Server.class)).parse(reader);
        Mockito.verify(factory).createWorkspace("W7-DENNIS-1", "W7-DENNIS-1", "dennis", "");
    }

    @Bug(4726)
    @Test
    public void assertNoIndexOutOfBoundsIsThrownSecondEdition() throws Exception {
        WorkspaceFactory factory = Mockito.mock(ListWorkspacesCommand.WorkspaceFactory.class);
        
        StringReader reader = new StringReader(
                "Server: xxxx-xxxx-010\n" +
                "Workspace                Owner        Computer      Comment\n" +
                "------------------------ ------------ ------------- ---------------------------\n" +
                "Hudson.JOBXXXXXXXXXXXXXX First.LastXX XXXX-XXXX-007\n");

        new ListWorkspacesCommand(factory, mock(Server.class)).parse(reader);
        Mockito.verify(factory).createWorkspace("Hudson.JOBXXXXXXXXXXXXXX", "XXXX-XXXX-007", "First.LastXX", "");
    }

    @Test public void logWithNoWorkspaces() throws IOException {

        ListWorkspacesCommand.log(new ArrayList<Workspace>(0), listener.getLogger());

        assertLog(
                "Workspace Owner Computer Comment",
                "--------- ----- -------- -------"
        );
    }

    @Test public void logWithManyWorkspaces() throws IOException {

        final ArrayList<Workspace> workspaces = new ArrayList<Workspace>();
        workspaces.add(new Workspace(null, "Hudson.JOBXXXXXXXXXXXXXX", "XXXX-XXXX-007", "First.LastXX", "This is a comment"));
        workspaces.add(new Workspace(null, "Hudson-newJob-MASTER", "COMPUTER", "jenkins-tfs-plugin", "Created by the Jenkins tfs-plugin functional tests."));

        ListWorkspacesCommand.log(workspaces, listener.getLogger());

        assertLog(
                "Workspace                Owner              Computer      Comment                                            ",
                "------------------------ ------------------ ------------- ---------------------------------------------------",
                "Hudson.JOBXXXXXXXXXXXXXX First.LastXX       XXXX-XXXX-007 This is a comment                                  ",
                "Hudson-newJob-MASTER     jenkins-tfs-plugin COMPUTER      Created by the Jenkins tfs-plugin functional tests."
        );
    }

    @Test public void logWithOneWorkspace() throws IOException {

        final ArrayList<Workspace> workspaces = new ArrayList<Workspace>(1);
        workspaces.add(new Workspace(null, "asterix", "ASTERIX", "redsolo_cp", "This is a comment"));

        ListWorkspacesCommand.log(workspaces, listener.getLogger());

        assertLog(
                "Workspace Owner      Computer Comment          ",
                "--------- ---------- -------- -----------------",
                "asterix   redsolo_cp ASTERIX  This is a comment"
        );
    }
}
