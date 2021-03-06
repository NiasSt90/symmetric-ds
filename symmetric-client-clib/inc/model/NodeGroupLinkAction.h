/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
#ifndef SYM_NODE_GROUP_LINK_ACTION_H
#define SYM_NODE_GROUP_LINK_ACTION_H

#include <stdlib.h>
#include "util/Date.h"
#include "util/StringUtils.h"

#define SYM_NODE_GROUP_LINK_ACTION_PUSH "P"
#define SYM_NODE_GROUP_LINK_ACTION_WAIT_FOR_PULL "W"
#define SYM_NODE_GROUP_LINK_ACTION_ROUTE "R"

typedef enum SymNodeGroupLinkAction {
    SymNodeGroupLinkAction_P,
    SymNodeGroupLinkAction_W,
    SymNodeGroupLinkAction_R
} SymNodeGroupLinkAction;

SymNodeGroupLinkAction SymNodeGroupLinkAction_fromCode(char *code);
char * SymNodeGroupLinkAction_toString(SymNodeGroupLinkAction);

#endif
