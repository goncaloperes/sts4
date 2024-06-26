/*******************************************************************************
 * Copyright (c) 2017, 2022 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.commons.java;

import java.net.URI;

public interface IJavaProject {

	final static String PROJECT_CACHE_FOLDER = ".sts4-cache";

	IClasspath getClasspath();
	IProjectBuild getProjectBuild();
	ClasspathIndex getIndex();
	URI getLocationUri();
	boolean exists();

	default String getElementName() {
		return getClasspath().getName();
	}

}
