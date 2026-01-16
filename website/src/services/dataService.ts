import api from "@/lib/api";

export const dataService = {
  exportData: async (format: "csv" | "json") => {
    const response = await api.get(`/data/export?format=${format}`, {
      responseType: "blob",
    });
    
    // Trigger download
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement("a");
    link.href = url;
    const date = new Date().toISOString().split("T")[0];
    link.setAttribute("download", `paisa_export_${date}.${format}`);
    document.body.appendChild(link);
    link.click();
    link.parentNode?.removeChild(link);
  },

  importData: async (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    
    const response = await api.post("/data/import", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    return response.data;
  },
};
