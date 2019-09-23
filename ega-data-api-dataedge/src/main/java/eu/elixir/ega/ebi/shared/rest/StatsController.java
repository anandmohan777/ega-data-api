/*
 * Copyright 2016 ELIXIR EGA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.elixir.ega.ebi.shared.rest;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Set;

import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * @author asenf
 */
@Slf4j
@RestController
@RequestMapping("/stats")
public class StatsController {

    /**
     * Obtain local CPU Load (used by EBI Load Balancer as Heartbeat)
     *
     * @return the current CPU load (in percent) as an integer
     */
    private static double getProcessCpuLoad() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
        AttributeList list = mbs.getAttributes(name, new String[]{"ProcessCpuLoad"});

        if (list.isEmpty()) return Double.NaN;

        Attribute att = (Attribute) list.get(0);
        Double value = (Double) att.getValue();

        if (value == -1.0) return Double.NaN;
        return ((int) (value * 1000) / 10.0);
    }

    /**
     * OPTIONS endpoint for /load
     *
     * @param response http response to modify
     */
    @RequestMapping(value = "/load", method = OPTIONS)
    public void getLoadOptions(HttpServletResponse response) {
        response.addHeader("Access-Control-Request-Method", "GET");
        log.info("Adding Header load");
    }

    /**
     * Returns the string result of getProcessCpuLoad() to GET requests.
     *
     * @return the current CPU load in string format.
     */
    @RequestMapping(value = "/load", method = GET)
    @ResponseBody
    public String get() {

        String load;
        try {
            load = String.valueOf(getProcessCpuLoad());
        } catch (Exception ex) {
            load = "Error";
        }

        return load;
    }

    /**
     * Test endpoint for OPTIONS requests
     *
     * @param response http response to modify
     */
    @RequestMapping(value = "/testme", method = OPTIONS)
    public void getTestOptions(HttpServletResponse response) {
        response.addHeader("Access-Control-Request-Method", "GET");
        log.info("Adding Header testme");
    }

    /**
     * Test endpoint for checking headers and permissions for GET and POST
     * requests.
     *
     * @param servletRequest the http request
     * @param headers headers for the request
     * @return A test page listing headers, Permissions, and request origin
     */
    @RequestMapping(value = "/testme", method = {GET, POST})
    @ResponseBody
    public String testme(HttpServletRequest servletRequest, @RequestHeader HttpHeaders headers) {

        String result;

        result = "Headers: ";
        Set<String> keySet = headers.keySet();
        for (String k : keySet) {
            log.info(k);
            result += k + " ";
            log.info(k + ": " + headers.get(k));
        }

        if (headers.containsKey("X-Permissions")) {
            result += "\nPermissions: ";
            List<String> get = headers.get("X-Permissions");
            for (String g : get) {
                result += g + " ";
            }
        }

        result += "\nOrigin: " + servletRequest.getRemoteAddr() + "\n";

        return result;
    }

}
