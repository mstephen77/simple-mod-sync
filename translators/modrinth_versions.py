#!/bin/python3
"""
© 2025 oxydien | Free to use, share and modify

This file is part of the simple-mod-sync project.
A tool to help synchronize Minecraft content (mods, resourcepacks, datapacks, shaders) from Modrinth.
It reads version IDs from a text file and generates a JSON sync file containing download URLs and metadata.

Usage:
    1. Create a text file (default: versions.txt) containing Modrinth version IDs, one per line
        - ex.:
        KbH00yy8
        tAx0UOBX
        AABBCCDD
    2. Install required packages: `pip install requests`
    3. Run the script: `python3 modrinth_versions.py`
    4. The script will generate a sync file (default: sync.json) with all the necessary information
"""

### CONFIGURATION
# Base URL for the Modrinth API
modrinth_api_base_url = "https://api.modrinth.com/v2/"  # Use staging-api.modrinth.com for testing

# Input file containing version IDs, one per line
input_file = "versions.txt"

# Output JSON file that will contain the sync information
output_file = "sync.json"

### CODE
import enum
import json
import requests
from typing import List, Dict

# Enum to categorize different types of Minecraft content
ContentType = enum.Enum('ContentType', ['mod', 'resourcepack', 'datapack', 'shader'])


class Content:
    """
    Represents a single piece of Minecraft content (mod, resourcepack, etc.) to be synced.

    Attributes:
        url (str): Direct download URL for the content
        name (str): Display name of the content (from project title)
        version (str): Modrinth version ID
        type (ContentType): Category of content (mod, resourcepack, etc.)
    """
    url: str
    name: str
    version: str
    type: ContentType

    def __init__(self, url: str, name: str, version: str, type: ContentType):
        self.url = url
        self.name = name
        self.version = version
        self.type = type

    def __str__(self):
        return f"Content(url={self.url}, name={self.name}, version={self.version}, type={self.type})"

    def __dict__(self):
        """Convert the content object to a dictionary for JSON serialization"""
        return {
            "url": self.url,
            "name": self.name,
            "version": self.version,
            "type": self.type.name
        }


class SyncData:
    """
    Container for all content that needs to be synchronized.

    Attributes:
        version (int): Schema version of the sync data (default: 3)
        contents (List[Content]): List of all content to be synced
    """
    version: int
    contents: List[Content]

    def __init__(self, contents: List[Content], version: int = 3):
        self.version = version
        self.contents = contents
        self.archive = None

    def __str__(self):
        return f"SyncData(version={self.version}, contents={self.contents})"

    def add_content(self, content: Content):
        """Add a new piece of content to the sync list"""
        self.contents.append(content)

    def to_json(self):
        """Convert the sync data to a JSON string for saving to file"""
        return json.dumps({
            "sync_version": self.version,
            "sync": [content.__dict__() for content in self.contents]
        })


class Parser:
    """
    Handles parsing version IDs and fetching information from the Modrinth API.

    This class reads version IDs from a file, queries the Modrinth API for metadata,
    and generates a SyncData object with all the necessary information.
    """
    path: str
    versions: List[str]  # List of version IDs from input file
    id_map: Dict[str, str] = {}  # Maps version IDs to project IDs

    def __init__(self, path: str):
        self.path = path
        self.__read_file()
        self.__init_id_map()

    def parse(self) -> SyncData:
        """
        Main parsing function that coordinates the API calls and data processing.
        Returns a SyncData object containing all content information.
        """
        # Get file information for all versions
        files_info = self.__get_versions_files()
        # Get project names for all versions
        projects_info = self.__get_projects_info()

        # Create Content objects for each version
        contents = []
        for version in self.versions:
            if version in files_info:
                url = files_info[version]['url']
                type = files_info[version]['type']
                name = projects_info[self.id_map[version]]
                contents.append(Content(url, name, version, type))

        return SyncData(contents)

    def __read_file(self):
        """Read version IDs from the input file"""
        with open(self.path, "r") as f:
            data = f.read()
            self.versions = data.splitlines()

    def __init_id_map(self):
        """Initialize the version ID to project ID mapping dictionary"""
        for version in self.versions:
            self.id_map[version] = ""

    def __get_versions_files(self) -> Dict[str, any]:
        """
        Query the Modrinth API for version information.
        Returns a dictionary mapping version IDs to file URLs and content types.
        """
        ids = json.dumps(self.versions)
        url = f"{modrinth_api_base_url}versions?ids={ids}"

        response = requests.get(url)
        files = {}

        if response.status_code == 200:
            versions_info = response.json()

            for version in versions_info:
                version_id = version.get('id')
                self.id_map[version_id] = version.get('project_id', '')

                # Find the primary file's URL
                file_url = ""
                for file in version.get('files', []):
                    if file.get('primary'):
                        file_url = file.get('url', '')
                        break

                files[version_id] = {
                    "url": file_url,
                    "type": self.__get_project_type(version.get('loaders', []))
                }
        else:
            print(f"Failed to fetch versions info: {response.status_code}, {response.text}")

        return files

    def __get_project_type(self, loaders: List[str]) -> ContentType:
        """
        Determine the content type based on the loader information from Modrinth.
        Returns a ContentType enum value.
        """
        for loader in loaders:
            if loader in ["fabric", "quilt", "forge", "neoforge"]:
                return ContentType.mod
            elif loader in ["iris", "optifine"]:
                return ContentType.shader
            elif loader == "datapack":
                return ContentType.datapack
            elif loader == "minecraft":
                return ContentType.resourcepack

        # Default to mod if no specific loader is identified
        return ContentType.mod

    def __get_projects_info(self) -> Dict[str, str]:
        """
        Query the Modrinth API for project information.
        Returns a dictionary mapping project IDs to project names.
        """
        project_ids = [id for id in self.id_map.values() if id]
        ids = json.dumps(project_ids)
        url = f"{modrinth_api_base_url}projects?ids={ids}"

        response = requests.get(url)
        projects_info = {}

        if response.status_code == 200:
            projects = response.json()

            for project in projects:
                project_id = project.get('id')
                # Use title if available, fall back to slug if not
                project_name = project.get('title', project.get('slug', 'UNKNOWN'))
                projects_info[project_id] = project_name
        else:
            print(f"Failed to fetch projects info: {response.status_code}, {response.text}")

        return projects_info


def main():
    """
    Main entry point of the script.
    Reads version IDs, fetches information from Modrinth, and generates the sync file.
    """
    print("Reading versions from", input_file)
    parser = Parser(input_file)
    print("Generating sync file", output_file)
    try:
        sync_data = parser.parse()
        with open(output_file, "w") as f:
            f.write(sync_data.to_json())
    except Exception as e:
        print("Failed to generate sync file:", e)
    print("Done")


if __name__ == "__main__":
    main()
