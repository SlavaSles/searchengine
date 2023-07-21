package searchengine.services;

import lombok.*;
import searchengine.model.SiteEntity;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SiteAddress {
    private SiteEntity site;
    private String domain;
    private String subDomainUrl;
}
