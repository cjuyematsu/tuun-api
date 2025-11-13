package com.ccm.tuunapi.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.ccm.tuunapi.service.SpotifyService;

@RestController
@RequestMapping("/api/spotify")
public class SpotifyController {
    
    @Autowired
    private SpotifyService spotifyService;
    
    @GetMapping("/search")
    public ResponseEntity<?> searchTrack(@RequestParam String query) {
        return ResponseEntity.ok(spotifyService.searchTrack(query));
    }
    
    @GetMapping("/track/{id}")
    public ResponseEntity<?> getTrack(@PathVariable String id) {
        return ResponseEntity.ok(spotifyService.getTrackDetails(id));
    }
}