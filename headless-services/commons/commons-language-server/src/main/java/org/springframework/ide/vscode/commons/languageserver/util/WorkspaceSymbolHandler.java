/*******************************************************************************
 * Copyright (c) 2017 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.commons.languageserver.util;

import java.util.List;

import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolParams;

import com.google.common.collect.ImmutableList;

@FunctionalInterface
public interface WorkspaceSymbolHandler {

	WorkspaceSymbolHandler NO_SYMBOLS = (params) -> ImmutableList.of();

	List<? extends WorkspaceSymbol> handle(WorkspaceSymbolParams params);

}
