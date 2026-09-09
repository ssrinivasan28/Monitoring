package com.islandpacific.sentinel.integration.itsm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItsmStateMapperTest {

    @Test
    void toItsmState_servicenow_mapsAllLocalStatuses() {
        assertThat(ItsmStateMapper.toItsmState("servicenow", "open")).isEqualTo("1");
        assertThat(ItsmStateMapper.toItsmState("servicenow", "ack")).isEqualTo("2");
        assertThat(ItsmStateMapper.toItsmState("servicenow", "assigned")).isEqualTo("2");
        assertThat(ItsmStateMapper.toItsmState("servicenow", "resolved")).isEqualTo("6");
    }

    @Test
    void toLocalStatus_servicenow_mapsBackToLocalVocabulary() {
        assertThat(ItsmStateMapper.toLocalStatus("servicenow", "1")).isEqualTo("open");
        assertThat(ItsmStateMapper.toLocalStatus("servicenow", "2")).isEqualTo("ack");
        assertThat(ItsmStateMapper.toLocalStatus("servicenow", "6")).isEqualTo("resolved");
        assertThat(ItsmStateMapper.toLocalStatus("servicenow", "7")).isEqualTo("resolved"); // Closed
    }

    @Test
    void toItsmState_jira_mapsAllLocalStatuses() {
        assertThat(ItsmStateMapper.toItsmState("jira", "open")).isEqualTo("To Do");
        assertThat(ItsmStateMapper.toItsmState("jira", "ack")).isEqualTo("In Progress");
        assertThat(ItsmStateMapper.toItsmState("jira", "assigned")).isEqualTo("In Progress");
        assertThat(ItsmStateMapper.toItsmState("jira", "resolved")).isEqualTo("Done");
    }

    @Test
    void toLocalStatus_jira_mapsBackToLocalVocabulary() {
        assertThat(ItsmStateMapper.toLocalStatus("jira", "To Do")).isEqualTo("open");
        assertThat(ItsmStateMapper.toLocalStatus("jira", "In Progress")).isEqualTo("ack");
        assertThat(ItsmStateMapper.toLocalStatus("jira", "Done")).isEqualTo("resolved");
    }

    @Test
    void toLocalStatus_unrecognizedState_defaultsToOpen() {
        assertThat(ItsmStateMapper.toLocalStatus("servicenow", "999")).isEqualTo("open");
        assertThat(ItsmStateMapper.toLocalStatus("jira", "Some Custom Status")).isEqualTo("open");
    }

    @Test
    void toItsmState_unsupportedKind_throws() {
        assertThatThrownBy(() -> ItsmStateMapper.toItsmState("pagerduty", "open"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toItsmState_unrecognizedLocalStatus_throws() {
        assertThatThrownBy(() -> ItsmStateMapper.toItsmState("servicenow", "bogus"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
