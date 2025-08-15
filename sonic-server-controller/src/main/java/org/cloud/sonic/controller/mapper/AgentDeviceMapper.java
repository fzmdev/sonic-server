package org.cloud.sonic.controller.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.cloud.sonic.controller.models.domain.Devices;

import java.util.List;

@Mapper
public interface AgentDeviceMapper {

    @Select("select\n" +
            "\t CASE\n" +
            "\t\tWHEN d.platform = 1 THEN 'android'\n" +
            "\t\tWHEN d.platform = 2 THEN 'ios'\n" +
            "\tEND as device_platform,\n" +
            "\td.ud_id,\n" +
            "\td.version,\n" +
            "\ta.secret_key,\n" +
            "\ta.host,\n" +
            "\ta.port,\n" +
            "\ta.tidevice_socket\n" +
            "from\n" +
            "\tdevices d\n" +
            "inner join agents a \n" +
            "on\n" +
            "\td.agent_id = a.id\n" +
            "where\n" +
            "\td.status = \"ONLINE\"\n" +
            "\tand \n" +
            "\td.ud_id not like '%:%';")
    public List<Devices> findAgentAndDevice();
}
