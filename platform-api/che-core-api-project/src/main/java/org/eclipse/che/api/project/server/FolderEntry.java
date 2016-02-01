/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.project.server;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.vfs.Path;
import org.eclipse.che.api.vfs.VirtualFile;
import org.eclipse.che.api.vfs.VirtualFileFilter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Folder entry.
 *
 * @author andrew00x
 */
public class FolderEntry extends VirtualFileEntry {
    private static final VirtualFileFilter FOLDER_FILTER = new VirtualFileFilter() {
        @Override
        public boolean accept(VirtualFile file) {
            return file.isFolder();
        }
    };

    private static final VirtualFileFilter FILES_FILTER = new VirtualFileFilter() {
        @Override
        public boolean accept(VirtualFile file) {
            return file.isFile();
        }
    };

    private static final VirtualFileFilter FILE_FOLDER_FILTER = new VirtualFileFilter() {
        @Override
        public boolean accept(VirtualFile file) {
            return (file.isFile() || file.isFolder());
        }
    };

    public FolderEntry(VirtualFile virtualFile) {
        super(virtualFile);
    }

    @Override
    public FolderEntry copyTo(String newParent) throws NotFoundException, ForbiddenException, ConflictException, ServerException {
        return copyTo(newParent, getName(), false);
    }

    @Override
    public FolderEntry copyTo(String newParent, String name, boolean overWrite)
            throws NotFoundException, ForbiddenException, ConflictException, ServerException {
        final VirtualFile vf = getVirtualFile();
        //final MountPoint mp = vf.getMountPoint();
        return new FolderEntry(vf.copyTo(virtualFileByPath(newParent), name, overWrite));
    }

    /**
     * Get child by relative path.
     *
     * @param path
     *         relative path
     * @return child
     * @throws ForbiddenException
     *         if access to child item is forbidden
     * @throws ServerException
     *         if other error occurs
     */
    public VirtualFileEntry getChild(String path) throws ForbiddenException, ServerException {
        final VirtualFile child = getVirtualFile().getChild(Path.of(path));
        if (child == null) {
            return null;
        }
        if (child.isFile()) {
            return new FileEntry(child);
        }
        return new FolderEntry(child);
    }

    /**
     * Get children of this folder. If current user doesn't have read access to some child they aren't added in result list.
     *
     * @throws ServerException
     *         if an error occurs
     */
    public List<VirtualFileEntry> getChildren() throws ServerException {
        return getChildren(VirtualFileFilter.ACCEPT_ALL);
    }

    /**
     * Get child files of this folder. If current user doesn't have read access to some child they aren't added in result list.
     *
     * @throws ServerException
     *         if an error occurs
     */
    public List<FileEntry> getChildFiles() throws ServerException {
        List <VirtualFile> vfChildren = getVirtualFile().getChildren(FILES_FILTER);
        final List<FileEntry> children = new ArrayList<>();
        for(VirtualFile c : vfChildren) {
            children.add(new FileEntry(c));
        }
        return children;
    }

    /**
     * Gets child folders of this folder. If current user doesn't have read access to some child they aren't added in result list.
     *
     * @throws ServerException
     *         if an error occurs
     */
    public List<FolderEntry> getChildFolders() throws ServerException {
        List <VirtualFile> vfChildren = getVirtualFile().getChildren(FOLDER_FILTER);
        final List<FolderEntry> children = new ArrayList<>();
        for(VirtualFile c : vfChildren) {
            children.add(new FolderEntry(c));
        }
        return children;
    }

    /**
     * Gets child folders and files of this folder. If current user doesn't have read access to some child they aren't added in result
     * list.
     *
     * @throws ServerException
     *         if an error occurs
     */
    public List<VirtualFileEntry> getChildFoldersFiles() throws ServerException {
        return getChildren(FILE_FOLDER_FILTER);
    }

    List<VirtualFileEntry> getChildren(VirtualFileFilter filter) throws ServerException {

        final List<VirtualFile> vfChildren = getVirtualFile().getChildren(filter);

        final List<VirtualFileEntry> children = new ArrayList<>();
        for(VirtualFile vf : vfChildren) {
            if (vf.isFile()) {
                children.add(new FileEntry(vf));
            } else {
                children.add(new FolderEntry(vf));
            }
        }
        return children;
    }

    /**
     * Creates new file in this folder.
     *
     * @param name
     *         name
     * @param content
     *         content. In case of {@code null} empty file is created
     * @return newly create VirtualFile
     * @throws ForbiddenException
     *         if copy operation is forbidden
     * @throws ConflictException
     *         if operation causes conflict, e.g. name conflict
     * @throws ServerException
     *         if other error occurs
     * @see VirtualFile#createFile(String, InputStream)
     */
    public FileEntry createFile(String name, byte[] content)
            throws ForbiddenException, ConflictException, ServerException {
        if (isRoot(getVirtualFile())) {
            throw new ForbiddenException("Can't create file in root folder.");
        }
        return createFile(name, content == null ? null : new ByteArrayInputStream(content));
    }

    /**
     * Creates new file in this folder.
     *
     * @param name
     *         name
     * @param content
     *         content. In case of {@code null} empty file is created
     * @return newly create VirtualFile
     * @throws ForbiddenException
     *         if copy operation is forbidden
     * @throws ConflictException
     *         if operation causes conflict, e.g. name conflict
     * @throws ServerException
     *         if other error occurs
     * @see VirtualFile#createFile(String, InputStream)
     */
    public FileEntry createFile(String name, InputStream content)
            throws ForbiddenException, ConflictException, ServerException {
        if (isRoot(getVirtualFile())) {
            throw new ForbiddenException("Can't create file in root folder.");
        }
        return new FileEntry(getVirtualFile().createFile(name, content));
    }

    /**
     * Creates new VirtualFile which denotes folder and use this one as parent folder.
     *
     * @param name
     *         name. If name is string separated by '/' all nonexistent parent folders must be created.
     * @return newly create VirtualFile that denotes folder
     * @throws ForbiddenException
     *         if copy operation is forbidden
     * @throws ConflictException
     *         if item with specified {@code name} already exists
     * @throws ServerException
     *         if other error occurs
     */
    public FolderEntry createFolder(String name) throws ConflictException, ServerException, ForbiddenException {
        return new FolderEntry(getVirtualFile().createFolder(name));
    }

    /** Tests whether this FolderEntry is a root folder. */
    public boolean isRoot() {
        return isRoot(getVirtualFile());
    }

//    public boolean isProject() {
//
//    }

    private boolean isRoot(VirtualFile virtualFile) {
        return virtualFile.isRoot();
    }
}
